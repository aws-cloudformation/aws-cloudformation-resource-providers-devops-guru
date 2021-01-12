package software.amazon.devopsguru.notificationchannel;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.AddNotificationChannelRequest;
import software.amazon.awssdk.services.devopsguru.model.AddNotificationChannelResponse;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsResponse;
import software.amazon.awssdk.services.devopsguru.model.NotificationChannel;
import software.amazon.awssdk.services.devopsguru.model.ResourceNotFoundException;
import software.amazon.awssdk.services.devopsguru.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnServiceLimitExceededException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<DevOpsGuruClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> checkForPreCreateResourceExistence(request, progress, proxyClient))
                .then(progress ->
                        proxy.initiate("AWS-DevOpsGuru-NotificationChannel::Create", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToAddNotificationChannelRequest)
                                .makeServiceCall((awsRequest, client) -> createResource(awsRequest, client, model, callbackContext))
                                .progress())
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

    private AddNotificationChannelResponse createResource(
            final AddNotificationChannelRequest addNotificationChannelRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext){
        AddNotificationChannelResponse addNotificationChannelResponse = null;

        try {
        addNotificationChannelResponse = proxyClient.injectCredentialsAndInvokeV2(addNotificationChannelRequest, proxyClient.client()::addNotificationChannel);
        logger.log(String.format("PutNotificationChannel response: %s, ", addNotificationChannelResponse.toString()));
        model.setId(addNotificationChannelResponse.id());
        } catch (final AccessDeniedException e) {
            throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        } catch (final ResourceNotFoundException e) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getConfig().toString(), e);
        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        } catch (final ServiceQuotaExceededException e) {
            throw new CfnServiceLimitExceededException(ResourceModel.TYPE_NAME, "ServiceQuotaExceeded", e);
        }  catch (final Exception e) {
            throw new CfnInternalFailureException(e);
        }

        return addNotificationChannelResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> checkForPreCreateResourceExistence(
            final ResourceHandlerRequest<ResourceModel> request,
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final ProxyClient<DevOpsGuruClient> proxyClient) {
        final ResourceModel model = progressEvent.getResourceModel();
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        String topicArn = model.getConfig().getSns().getTopicArn();

        ListNotificationChannelsResponse listNotificationChannelsResponse = listNotificationChannel(Translator.translateToListNotificationChannelRequest(model), proxyClient);
        for (NotificationChannel channel : listNotificationChannelsResponse.channels()) {
            if (channel.config().sns().topicArn().equals(topicArn)) {
                throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, topicArn);
            }
        }

        return ProgressEvent.progress(model, callbackContext);
    }
}
