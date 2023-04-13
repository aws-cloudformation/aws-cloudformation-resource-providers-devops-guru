package software.amazon.devopsguru.resourcecollection;

import com.google.common.collect.Lists;
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.ResourceCollectionType;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.*;

import java.util.ArrayList;
import java.util.List;

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
        final String resourceCollectionType = model.getResourceCollectionType();
        List<GetResourceCollectionRequest> resourceCollectionRequests = generateResourceCollectionRequests(
                resourceCollectionType,
                logger);

        List<GetResourceCollectionResponse> resourceCollectionResponses = new ArrayList<>();
        for (GetResourceCollectionRequest resourceCollectionRequest: resourceCollectionRequests) {
            try {
                model.setResourceCollectionType(resourceCollectionRequest.resourceCollectionTypeAsString());
                logger.log(String.format("[ListHandler] setting ResourceCollectionType: %s, " +
                        "invoking getSingleResourceCollection",
                        resourceCollectionRequest.resourceCollectionTypeAsString()));
                GetResourceCollectionResponse resourceCollectionResponse = getSingleResourceCollection(
                        resourceCollectionRequest,
                        proxyClient,
                        model
                );
                logger.log(String.format("[ListHandler] Successfully invoked getSingleResourceCollection with" +
                                "ResourceCollectionType: %s",
                        resourceCollectionRequest.resourceCollectionTypeAsString()));
                resourceCollectionResponses.add(resourceCollectionResponse);
            }
            catch (CfnNotFoundException e) {
                logger.log(String.format("[ListHandler] Expected behavior catching CfnNotFoundException: %s",e));
            }
        }
        // ideal situation: resourceCollectionResponses has one element
        if (resourceCollectionResponses.size() > 1) {
            throw new CfnServiceInternalErrorException("Cannot have more than 1 ResourceCollection(s)");
        }

        GetResourceCollectionResponse response = null;
        final boolean isEmptyResourceCollectionResponses = resourceCollectionResponses.size() == 0;
        if (isEmptyResourceCollectionResponses) {
            logger.log("No valid ResourceCollection is found.");
        } else {
            logger.log("Exact one ResourceCollection is found.");
            response = resourceCollectionResponses.get(0);
        }
        final String nextToken = response == null ? null : response.nextToken();
        final List<ResourceModel> models = Translator.translateFromListResponse(response);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .nextToken(nextToken)
                .build();
    }

    private List<GetResourceCollectionRequest> generateResourceCollectionRequests(
            final String resourceCollectionType,
            final Logger logger) {

        if (resourceCollectionType != null) {
            final GetResourceCollectionRequest request = getResourceCollectionRequestWithResourceCollectionType(
                    resourceCollectionType
            );
            logger.log(String.format("Built GetResourceCollectionRequest from retrieved ResourceCollectionType: %s",
                    resourceCollectionType));
            return Lists.newArrayList(request);
        }
        final GetResourceCollectionRequest requestWithCloudFormationFilter =
                getResourceCollectionRequestWithResourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION);
        final GetResourceCollectionRequest requestWithTagsFilter =
                getResourceCollectionRequestWithResourceCollectionType(ResourceCollectionType.AWS_TAGS);
        logger.log("Unable to retrieve ResourceCollectionType from resource model, " +
                "built 2 GetResourceCollectionRequest(s)");
        return Lists.newArrayList(requestWithCloudFormationFilter, requestWithTagsFilter);
    }
}
