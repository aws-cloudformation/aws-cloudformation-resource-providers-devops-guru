package software.amazon.devopsguru.notificationchannel;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsResponse;
import software.amazon.awssdk.services.devopsguru.model.NotificationChannel;
import software.amazon.awssdk.services.devopsguru.model.RemoveNotificationChannelRequest;
import software.amazon.awssdk.services.devopsguru.model.RemoveNotificationChannelResponse;
import software.amazon.awssdk.services.devopsguru.model.ResourceNotFoundException;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<DevOpsGuruClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> checkForPreDeleteResourceExistence(request, progress, proxyClient))
                .then(progress ->
                        proxy.initiate("AWS-DevOpsGuru-NotificationChannel::Delete", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToRemoveNotificationChannelRequest)
                                .makeServiceCall((awsRequest, client) -> deleteResource(awsRequest, client, model))
                                .progress()
                )
                .then(progress -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.SUCCESS)
                        .build());
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param deleteNotificationChannelRequest the aws service request to delete a resource
     * @param proxyClient                   the aws service client to make the call
     * @return delete resource response
     */
    private RemoveNotificationChannelResponse deleteResource(
            final RemoveNotificationChannelRequest deleteNotificationChannelRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model){
        RemoveNotificationChannelResponse awsResponse = null;

        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(deleteNotificationChannelRequest, proxyClient.client()::removeNotificationChannel);
            logger.log(String.format("DeleteNotificationChannel response: %s", awsResponse.toString()));
        } catch (final AccessDeniedException e) {
            throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getId(), e);
        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> checkForPreDeleteResourceExistence(
            final ResourceHandlerRequest<ResourceModel> request,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final ProxyClient<DevOpsGuruClient> proxyClient) {
        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext callbackContext = progressEvent.getCallbackContext();

        ListNotificationChannelsResponse listNotificationChannelsResponse = listNotificationChannel(Translator.translateToListNotificationChannelRequest(model), proxyClient);
        for (NotificationChannel channel : listNotificationChannelsResponse.channels()) {
            if (channel.id().equals(model.getId())) {
                return ProgressEvent.progress(model, callbackContext);
            }
        }

        throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getId());
    }
}
