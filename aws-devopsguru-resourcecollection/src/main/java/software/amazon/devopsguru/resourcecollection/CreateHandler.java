package software.amazon.devopsguru.resourcecollection;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.ResourceCollectionType;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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

        if(model.getResourceCollectionFilter().getCloudFormation() != null
                && model.getResourceCollectionFilter().getTags() == null) {
            model.setResourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.name());
        }

        if(model.getResourceCollectionFilter().getCloudFormation() == null
                && model.getResourceCollectionFilter().getTags() != null) {
            model.setResourceCollectionType(ResourceCollectionType.AWS_TAGS.name());
        }

        if(model.getResourceCollectionType().isEmpty() ||
                (model.getResourceCollectionFilter().getCloudFormation() != null
                        && model.getResourceCollectionFilter().getTags() != null)) {
            throw new CfnInvalidRequestException("Invalid input, you can only have Tags or CloudFormation filter");
        }

        // TODO: Confirm with Uluru if the Create Handler could update the resources users created at console
        return ProgressEvent.progress(model, callbackContext)
                .then(progress ->
                        proxy.initiate("AWS-DevOpsGuru-ResourceCollection::Create", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToAddResourceCollectionRequest)
                                .makeServiceCall((awsRequest, client) -> createResource(awsRequest, client, model, logger))
                                .progress())
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));

    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param awsRequest
     * @param client
     * @param model
     * @param logger
     * @return
     */
    private UpdateResourceCollectionResponse createResource (
            final UpdateResourceCollectionRequest awsRequest,
            final ProxyClient<DevOpsGuruClient> client,
            final ResourceModel model,
            final Logger logger) {
        UpdateResourceCollectionResponse awsResponse = null;
        awsResponse = updateResourceCollection(awsRequest, client, model, logger, true);
        logger.log(String.format("%s successfully created. Generated UpdateResourceCollection response: %s", ResourceModel.TYPE_NAME, awsResponse.toString()));
        return awsResponse;
    }
}
