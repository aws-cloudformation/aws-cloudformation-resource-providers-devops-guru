package software.amazon.devopsguru.notificationchannel;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsRequest;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

import static software.amazon.devopsguru.notificationchannel.constants.FilterConstants.INSIGHT_SEVERITIES;
import static software.amazon.devopsguru.notificationchannel.constants.FilterConstants.MESSAGE_TYPES;

public class ListHandler extends BaseHandlerStd {
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<DevOpsGuruClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        final String awsAccountId = request.getAwsAccountId();

        return proxy.initiate("AWS-DevOpsGuru-NotificationChannel::List", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToListNotificationChannelRequest)
                .makeServiceCall((awsRequest, client) -> listResource(awsRequest, client, model, awsAccountId, request.getNextToken()))
                .done(this::constructResourceModelFromResponse);
    }

    private ListNotificationChannelsResponse listResource(
            final ListNotificationChannelsRequest listNotificationChannelsRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model,
            final String awsAccountId,
            final String nextToken){
        ListNotificationChannelsResponse listNotificationChannelsResponse = null;

        logger.log(String.format("ListNotificationChannelsRequest: %s",listNotificationChannelsRequest.toString()));
        listNotificationChannelsResponse = listNotificationChannel(listNotificationChannelsRequest, proxyClient);

        logger.log(String.format("%d \"%s\" for accountId [%s] has been successfully listed for token %s!", listNotificationChannelsResponse.channels().size(), ResourceModel.TYPE_NAME, awsAccountId, nextToken));

        return listNotificationChannelsResponse;
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            final ListNotificationChannelsResponse awsResponse) {
        final List<ResourceModel> models = new ArrayList<>();

        awsResponse.channels().forEach(channel -> {
            if(channel.config().sns() != null) {
                String topicArn = channel.config().sns().topicArn();
                List<String> severities = processNotificationFilters(INSIGHT_SEVERITIES, channel.config().filters());
                List<String> messageTypes = processNotificationFilters(MESSAGE_TYPES, channel.config().filters());

                models.add(ResourceModel.builder()
                        .config(NotificationChannelConfig.builder()
                                .sns(new SnsChannelConfig(topicArn))
                                .filters(new NotificationFilterConfig(severities, messageTypes))
                                .build())
                        .id(channel.id())
                        .build());
            }
        });

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .nextToken(awsResponse.nextToken())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
