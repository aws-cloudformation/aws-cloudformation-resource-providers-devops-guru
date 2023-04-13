package software.amazon.devopsguru.resourcecollection;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.CloudFormationCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.ResourceCollectionType;
import software.amazon.awssdk.services.devopsguru.model.TagCollectionFilter;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.List;

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
        logger.log(String.format("check ResourceModel from ReadHandler: %s", model.toString()));

        return proxy.initiate("AWS-DevOpsGuru-ResourceCollection::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
                .translateToServiceRequest(Translator::translateToGetResourceCollectionRequest)
                .makeServiceCall((awsRequest, client) -> readResource(awsRequest, client, model))
                .done(this::constructResourceModelFromResponse);
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param getResourceCollectionRequest the aws service request to describe a resource
     * @param proxyClient the aws service client to make the call
     * @return describe resource response
     */
    private GetResourceCollectionResponse readResource(
            final GetResourceCollectionRequest getResourceCollectionRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model) {
        GetResourceCollectionResponse awsResponse = null;
        if(getResourceCollectionRequest.resourceCollectionType().equals(ResourceCollectionType.AWS_CLOUD_FORMATION)){
            List<String> awsResponseStackNames = getAllCloudFormationResourceCollection(getResourceCollectionRequest, proxyClient, model, logger);
            awsResponse = GetResourceCollectionResponse.builder().resourceCollection(
                    ResourceCollectionFilter.builder()
                            .cloudFormation(
                                    CloudFormationCollectionFilter.builder()
                                            .stackNames(awsResponseStackNames)
                                            .build())
                            .build())
                    .build();
        }
        if(getResourceCollectionRequest.resourceCollectionType().equals(ResourceCollectionType.AWS_TAGS)){
            TagCollection allTagCollection = getAllTagsResourceCollection(getResourceCollectionRequest, proxyClient, model, logger);
            awsResponse = GetResourceCollectionResponse.builder().resourceCollection(
                    ResourceCollectionFilter.builder()
                            .tags(Arrays.asList(
                                    TagCollectionFilter.builder()
                                    .appBoundaryKey(allTagCollection.getAppBoundaryKey())
                                    .tagValues(allTagCollection.getTagValues())
                                    .build()
                            ))
                            .build())
                    .build();
        }
        logger.log(String.format("GetResourceCollection response: %s. %s has successfully been read.", awsResponse.toString(), ResourceModel.TYPE_NAME));
        return awsResponse;
    }

    /**
     * Implement client invocation of the read request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     * @param awsResponse the aws service describe resource response
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> constructResourceModelFromResponse(
            final GetResourceCollectionResponse awsResponse) {
        return ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse));
    }
}
