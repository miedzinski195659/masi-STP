package pl.lodz.p.it.masi.stp.chatbot.services;

import com.ibm.watson.developer_cloud.conversation.v1.Conversation;
import com.ibm.watson.developer_cloud.conversation.v1.model.InputData;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageOptions;
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.lodz.p.it.masi.stp.chatbot.amazon.*;
import pl.lodz.p.it.masi.stp.chatbot.dtos.MessageDto;
import pl.lodz.p.it.masi.stp.chatbot.model.collections.conversation.ConversationHelper;
import pl.lodz.p.it.masi.stp.chatbot.model.enums.*;
import pl.lodz.p.it.masi.stp.chatbot.repositories.ConversationHelpersRepository;
import pl.lodz.p.it.masi.stp.chatbot.utils.CategoryUtils;
import pl.lodz.p.it.masi.stp.chatbot.utils.EnumUtils;

import javax.annotation.PostConstruct;
import javax.xml.ws.WebServiceException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements ConversationService {

    private static Logger logger = LoggerFactory.getLogger(ConversationServiceImpl.class);

    private final ConversationHelpersRepository helpers;

    private Conversation conversation;

    @Value("${amazon.secret.key}")
    private String amazonSecretKey;

    @Value("${amazon.access.key}")
    private String amazonAccessKey;

    @Value("${amazon.associate.tag}")
    private String amazonAssociateTag;

    @Value("${watson.version.date}")
    private String watsonVersionDate;

    @Value("${watson.username}")
    private String watsonUsername;

    @Value("${watson.password}")
    private String watsonPassword;

    @Value("${watson.endpoint}")
    private String watsonEndpoint;

    @Autowired
    public ConversationServiceImpl(ConversationHelpersRepository helpers) {
        this.helpers = helpers;
    }

    @PostConstruct
    public void initialize() {
        conversation = new Conversation(watsonVersionDate, watsonUsername, watsonPassword);
        conversation.setEndPoint(watsonEndpoint);
    }

    @Override
    public MessageDto processMessage(MessageDto requestMsg) {
        MessageDto responseMsg = new MessageDto();
        MessageResponse watsonResponse = getWatsonResponse(requestMsg, responseMsg);
        getAmazonResponse(responseMsg, watsonResponse);
        return responseMsg;
    }

    public MessageResponse getWatsonResponse(MessageDto request, MessageDto response) {
        String workspaceId = "fb1afa02-f113-446c-ba28-a86992500910";
        InputData input = new InputData.Builder(request.getMessage()).build();
        MessageOptions options = new MessageOptions.Builder(workspaceId)
                .input(input)
                .context(request.getContext())
                .build();
        MessageResponse watsonResponse = conversation.message(options).execute();
        response.setContext(watsonResponse.getContext());
        response.setResponse(watsonResponse.getOutput().getText());
        logger.info(response.toString());
        return watsonResponse;
    }

    public void getAmazonResponse(MessageDto response, MessageResponse watsonResponse) {
        ConversationHelper currentConversationHelper = createOrLoadConversationHelper(
                watsonResponse.getContext().getConversationId()
        );

        Set<CategoriesEnum> categories = new HashSet<>();
        Set<AuthorsEnum> authors = new HashSet<>();
        Set<TitlesEnum> titles = new HashSet<>();
        Set<KeywordsEnum> keywords = new HashSet<>();
        Set<SortsEnum> sorts = new HashSet<>();

        EnumUtils.parseEntities(watsonResponse, categories, titles, authors, keywords, sorts);
        setCategory(currentConversationHelper, categories);
        ItemSearchRequest itemSearchRequest = createItemSearchRequest(currentConversationHelper, authors, titles, keywords, sorts);
        ItemSearchResponse amazonResponse = getItemSearchResponse(itemSearchRequest);
        setResponseUrl(response, itemSearchRequest, amazonResponse);
    }

    private ConversationHelper createOrLoadConversationHelper(String conversationId) {
        if (helpers.existsByConversationId(conversationId)) {
            return helpers.findByConversationId(conversationId);
        } else {
            return helpers.save(new ConversationHelper(conversationId));
        }
    }

    private void setCategory(ConversationHelper currentConversationHelper, Set<CategoriesEnum> categories) {
        CategoriesEnum category = CategoryUtils.findDeepestCategory(categories);
        if (category != null) {
            currentConversationHelper.setCategory(category);
            helpers.save(currentConversationHelper);
        }
    }

    private ItemSearchRequest createItemSearchRequest(ConversationHelper currentConversationHelper, Set<AuthorsEnum> authors,
                                                      Set<TitlesEnum> titles, Set<KeywordsEnum> keywords, Set<SortsEnum> sorts) {
        ItemSearchRequest itemSearchRequest = new ItemSearchRequest();
        itemSearchRequest.setSearchIndex(CategoriesEnum.ALL_BOOKS.getName());

        if (CollectionUtils.isNotEmpty(keywords)) {
            itemSearchRequest.setKeywords(String.join(" ", keywords.stream().map(KeywordsEnum::getPhrase).collect(Collectors.toList())));
        }

        if (CollectionUtils.isNotEmpty(authors)) {
            itemSearchRequest.setAuthor(String.join(" ", authors.stream().map(AuthorsEnum::getAuthor).collect(Collectors.toList())));
        }

        if (CollectionUtils.isNotEmpty(titles)) {
            itemSearchRequest.setTitle(String.join(" ", titles.stream().map(TitlesEnum::getTitle).collect(Collectors.toList())));
        }

        if (CollectionUtils.isNotEmpty(sorts)) {
            itemSearchRequest.setSort(sorts.toArray(new SortsEnum[0])[0].getValue());
        }

        if (currentConversationHelper.getCategory() != null) {
            itemSearchRequest.setBrowseNode(currentConversationHelper.getCategory().getBrowseNodeId());
        }

        return itemSearchRequest;
    }

    private ItemSearchResponse getItemSearchResponse(ItemSearchRequest itemSearchRequest) {
        AWSECommerceService service = new AWSECommerceService();
        service.setHandlerResolver(new AwsHandlerResolver(amazonSecretKey));

        AWSECommerceServicePortType port = service.getAWSECommerceServicePort();

        ItemSearch itemSearch = new ItemSearch();
        itemSearch.setAWSAccessKeyId(amazonAccessKey);
        itemSearch.setAssociateTag(amazonAssociateTag);
        itemSearch.getRequest().add(itemSearchRequest);

        ItemSearchResponse amazonResponse = null;
        try {
            amazonResponse = port.itemSearch(itemSearch);
        } catch (WebServiceException exc) {
            logger.error(exc.toString());
        }
        return amazonResponse;
    }

    private void setResponseUrl(MessageDto response, ItemSearchRequest itemSearchRequest, ItemSearchResponse amazonResponse) {
        if (amazonResponse != null) {
            logger.info(amazonResponse.toString());
            List<Items> receivedItems = amazonResponse.getItems();
            if (CollectionUtils.isNotEmpty(receivedItems)) {
                if (StringUtils.isNoneEmpty(itemSearchRequest.getKeywords())
                        || StringUtils.isNoneEmpty(itemSearchRequest.getTitle())
                        || StringUtils.isNoneEmpty(itemSearchRequest.getSort())) {
                    List<Item> items = receivedItems.get(0).getItem();
                    if (CollectionUtils.isNotEmpty(items)) {
                        response.setUrl(items.get(0).getDetailPageURL());
                    } else {
                        response.setUrl(receivedItems.get(0).getMoreSearchResultsUrl());
                        response.getResponse().clear();
                        response.getResponse().add("I am sorry, but i couldn't find what you are looking for. Try other keyword, title or author.");
                    }
                } else {
                    response.setUrl(receivedItems.get(0).getMoreSearchResultsUrl());
                }
            }
        }
    }
}
