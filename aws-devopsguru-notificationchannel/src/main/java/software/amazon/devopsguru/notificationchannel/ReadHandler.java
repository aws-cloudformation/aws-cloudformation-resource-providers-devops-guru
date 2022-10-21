package software.amazon.devopsguru.notificationchannel;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsResponse;
import software.amazon.awssdk.services.devopsguru.model.NotificationChannel;
import software.amazon.awssdk.services.devopsguru.model.NotificationFilterConfig;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.devopsguru.notificationchannel.constants.FilterConstants.INSIGHT_SEVERITIES;
import static software.amazon.devopsguru.notificationchannel.constants.FilterConstants.MESSAGE_TYPES;


public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return proxy.initiate("AWS-DevOpsGuru-NotificationChannel::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToListNotificationChannelRequest)
                .makeServiceCall((awsRequest, client) -> listNotificationChannel(awsRequest, client))
                .done((awsResponse) -> constructResourceModelFromResponse(awsResponse, model));
    }

    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            final ListNotificationChannelsResponse awsResponse,
            final ResourceModel model) {
        for(NotificationChannel channel: awsResponse.channels()) {
            if(channel.config().sns() != null && channel.id().equals(model.getId())) {
                NotificationFilterConfig filters = channel.config().filters();
                return ProgressEvent.defaultSuccessHandler(
                        (Translator.translateFromReadResponse(
                                channel.config().sns().topicArn(),
                                model.getId(),
                                processNotificationFilters(INSIGHT_SEVERITIES,filters),
                                processNotificationFilters(MESSAGE_TYPES, filters))));
            }
        }

        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getId());
    }
}
