package software.amazon.devopsguru.loganomalydetectionintegration;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationResponse;
import software.amazon.awssdk.services.devopsguru.model.UpdateServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.UpdateServiceIntegrationResponse;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


import static software.amazon.devopsguru.loganomalydetectionintegration.Constants.OptInStatusConstant;

public class DeleteHandler extends BaseHandlerStd {

    private Logger logger;

    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        this.createCloudWatchLogsProxyClient(proxy, cloudWatchLogsClient);
        final String accountId = request.getAwsAccountId();
        final ResourceModel model = request.getDesiredResourceState();

        // to behave like other resources, they need to read using the actual primaryIdentifier, not a random value
        if (!accountId.equals(model.getAccountId())) {
            logger.log(String.format("[DELETE] LogAnomalyDetectionIntegration not found for %s, will throw CfnNotFoundException",
                    model.getAccountId()));
            throw new CfnNotFoundException(model.TYPE_NAME, model.getPrimaryIdentifier().toString());
        }


        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> checkForTagLogGroupPermissions(progress, proxy, cloudWatchLogsProxyClient))
                .then(progress -> checkLogAnomalyDetectionDisabled(progress, proxyClient)
                )
                .then(progress ->
                        proxy.initiate("AWS-DevOpsGuru-ServiceIntegration::Delete", proxyClient, model, callbackContext)
                                .translateToServiceRequest(Translator::translateToDeleteServiceIntegrationRequest)
                                .makeServiceCall((awsRequest, client) -> deleteResource(awsRequest, client, model, logger))
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
    private UpdateServiceIntegrationResponse deleteResource (
            final UpdateServiceIntegrationRequest awsRequest,
            final ProxyClient<DevOpsGuruClient> proxyClient,
            final ResourceModel model,
            final Logger logger) {

        UpdateServiceIntegrationResponse awsResponse = null;
        awsResponse = updateServiceIntegration(awsRequest, proxyClient);
        logger.log(String.format("%s successfully deleted. Generated UpdateServiceIntegration response: %s", ResourceModel.TYPE_NAME, awsResponse.toString()));
        return awsResponse;
    }

    private ProgressEvent<ResourceModel, CallbackContext> checkLogAnomalyDetectionDisabled(
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
            final ProxyClient<DevOpsGuruClient> proxyClient
    ) {
        final CallbackContext callbackContext = progressEvent.getCallbackContext();
        final ResourceModel model = progressEvent.getResourceModel();
        final DescribeServiceIntegrationRequest describeServiceIntegrationRequest = Translator.translateToReadServiceIntegrationRequest(model);
        final DescribeServiceIntegrationResponse response = describeServiceIntegration(describeServiceIntegrationRequest, proxyClient);

        if (response.serviceIntegration().logsAnomalyDetection() == null ||
                response.serviceIntegration().logsAnomalyDetection().optInStatus() == null ||
                OptInStatusConstant.DISABLED.equals(response.serviceIntegration().logsAnomalyDetection().optInStatusAsString())) {
            String message = "[DELETE] LogAnomalyDetectionIntegration is already [%s]. Delete LogAnomalyDetectionIntegration failed.";
            logger.log(String.format(message, OptInStatusConstant.DISABLED));

            return ProgressEvent.failed(
                    model,
                    callbackContext,
                    HandlerErrorCode.NotFound,
                    String.format("LogAnomalyDetectionIntegration is already %s.", OptInStatusConstant.DISABLED));
        }
        return progressEvent;
    }
}
