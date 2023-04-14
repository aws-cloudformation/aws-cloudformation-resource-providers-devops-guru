package software.amazon.devopsguru.resourcecollection;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.TagCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.ValidationException;
import software.amazon.awssdk.services.devopsguru.model.ResourceCollectionType;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
        logger.log(String.format("check ResourceModel from DeleteHandler: %s", model.toString()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-DevOpsGuru-ResourceCollection::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                                .translateToServiceRequest(Translator::translateToRemoveCloudFormationResourceCollectionRequest)
                                .backoffDelay(BACKOFF_STRATEGY)
                                .makeServiceCall((awsRequest, client) -> deleteResource(awsRequest, client, model, callbackContext))
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
     * @param updateResourceCollectionRequest the aws service request to delete a resource
     * @param proxyClient                   the aws service client to make the call
     * @return delete resource response
     */
    private UpdateResourceCollectionResponse deleteResource(
            UpdateResourceCollectionRequest updateResourceCollectionRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model,
            final CallbackContext callbackContext){
        UpdateResourceCollectionResponse awsResponse = null;

        if(model.getResourceCollectionType() == null) {
            throw new CfnInvalidRequestException("Input request is invalid, missing ResourceCollectionType ");
        }

        try {
            GetResourceCollectionRequest getResourceCollectionRequest = GetResourceCollectionRequest.builder()
                    .resourceCollectionType(ResourceCollectionType.fromValue(model.getResourceCollectionType()))
                    .nextToken(null)
                    .build();
            checkIsEmptyResourceCollection(getResourceCollectionRequest, proxyClient, model, logger);
            // for TAGS, we need get AppBoundaryKey to reconstruct the request
            if(ResourceCollectionType.AWS_TAGS.name().equals(model.getResourceCollectionType())) {
                GetResourceCollectionResponse getResourceCollectionResponse = getSingleResourceCollection(getResourceCollectionRequest, proxyClient, model);
                   TagCollectionFilter tagCollection = getResourceCollectionResponse.resourceCollection().tags().get(0);
                   updateResourceCollectionRequest = Translator.translateToRemoveTagsResourceCollectionRequest(tagCollection.appBoundaryKey());
            }
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(updateResourceCollectionRequest, proxyClient.client()::updateResourceCollection);;
            logger.log(String.format("PutResourceFilter response: %s", awsResponse.toString()));
        } catch (final AccessDeniedException e) {
            throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        } catch (final ValidationException e) {
            throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
        }

        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
        return awsResponse;
    }
}
