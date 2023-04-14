package software.amazon.devopsguru.loganomalydetectionintegration;

// DevOps Guru import
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationResponse;
import software.amazon.awssdk.services.devopsguru.model.UpdateServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.UpdateServiceIntegrationResponse;

// CloudFormation import
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.devopsguru.loganomalydetectionintegration.Constants.OptInStatusConstant;


public class CreateHandler extends BaseHandlerStd {

    private Logger logger;
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        this.createCloudWatchLogsProxyClient(proxy, cloudWatchLogsClient);

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> checkForTagLogGroupPermissions(progress, proxy, cloudWatchLogsProxyClient))
                .then(progress -> checkLogAnomalyDetectionEnabled(progress, proxyClient)
                )
                .then(progress -> setPrimaryIdentifier(progress, request))
                .then(progress ->
                        proxy.initiate("AWS-DevOpsGuru-ServiceIntegration::Create", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToAddLogAnomalyDetectionIntegrationRequest)
                                .makeServiceCall((awsRequest, client) -> createResource(awsRequest, client, model, logger))
                                .progress())
                .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

    /**
     * Implement client invocation of the create request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param awsRequest
     * @param proxyClient
     * @param model
     * @param logger
     * @return
     */
    private UpdateServiceIntegrationResponse createResource (

            final UpdateServiceIntegrationRequest awsRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model,
            final Logger logger) {

        final UpdateServiceIntegrationResponse awsResponse = updateServiceIntegration(awsRequest, proxyClient);
        logger.log(String.format("%s successfully created. Generated UpdateServiceIntegration response: %s",
                ResourceModel.TYPE_NAME,
                awsResponse.toString()));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> setPrimaryIdentifier(
            ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            ResourceHandlerRequest<ResourceModel> request){

        ResourceModel model = progressEvent.getResourceModel();
        model.setAccountId(request.getAwsAccountId());
        progressEvent.setResourceModel(model);
        return progressEvent;
    }

    private ProgressEvent<ResourceModel, CallbackContext> checkLogAnomalyDetectionEnabled(
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final ProxyClient<DevOpsGuruClient> proxyClient
    ) {
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        final ResourceModel model = progressEvent.getResourceModel();
        final DescribeServiceIntegrationRequest describeServiceIntegrationRequest = Translator.translateToReadServiceIntegrationRequest(model);
        final DescribeServiceIntegrationResponse response = describeServiceIntegration(describeServiceIntegrationRequest, proxyClient);

        if (response.serviceIntegration().logsAnomalyDetection() != null &&
            OptInStatusConstant.ENABLED.equals(response.serviceIntegration().logsAnomalyDetection().optInStatusAsString())) {
            String message = "[CREATE] LogAnomalyDetectionIntegration is already [%s]. Create LogAnomalyDetectionIntegration failed.";
            logger.log(String.format(message, OptInStatusConstant.ENABLED));

            return ProgressEvent.failed(
                  model,
                  callbackContext,
                  HandlerErrorCode.AlreadyExists,
                  String.format("LogAnomalyDetectionIntegration is already %s.", OptInStatusConstant.ENABLED));
        }
     return progressEvent;
    }
}
