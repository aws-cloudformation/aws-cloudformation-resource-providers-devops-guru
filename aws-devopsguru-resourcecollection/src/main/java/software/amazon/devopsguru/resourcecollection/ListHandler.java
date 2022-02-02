package software.amazon.devopsguru.resourcecollection;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.*;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.*;

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

        final GetResourceCollectionRequest getResourceCollectionRequest =
                GetResourceCollectionRequest.builder()
                        .resourceCollectionType(ResourceCollectionType.AWS_CLOUD_FORMATION.getName())
                        .nextToken(request.getNextToken())
                        .build();

        try {
            GetResourceCollectionResponse  response =
                    proxyClient.injectCredentialsAndInvokeV2(getResourceCollectionRequest,
                            proxyClient.client()::getResourceCollection);

            logger.log(String.format("GetResourceFilter response: %s", response.toString()));

            List<ResourceModel> models = Translator.translateFromListResponse(response);

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModels(models)
                    .status(OperationStatus.SUCCESS)
                    .nextToken(response.nextToken())
                    .build();

        } catch (final AccessDeniedException e) {
            throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
        } catch (final ThrottlingException e) {
            throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
        } catch (final InternalServerException e) {
            throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
        }
    }
}
