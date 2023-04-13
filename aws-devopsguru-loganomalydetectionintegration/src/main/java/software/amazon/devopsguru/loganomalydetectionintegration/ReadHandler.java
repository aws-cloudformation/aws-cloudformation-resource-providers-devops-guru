package software.amazon.devopsguru.loganomalydetectionintegration;

// DevOps Guru package
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationResponse;

// CloudFormation package
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static software.amazon.devopsguru.loganomalydetectionintegration.Constants.OptInStatusConstant;

public class ReadHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<DevOpsGuruClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        final String accountId = request.getAwsAccountId();
        final ResourceModel model = request.getDesiredResourceState();
        // read check according to
        if (!accountId.equals(model.getAccountId())) {
            logger.log(String.format("[READ] LogAnomalyDetectionIntegration not found for %s, will throw CfnNotFoundException",
                    model.getAccountId()));
            throw new CfnNotFoundException(model.TYPE_NAME, model.getPrimaryIdentifier().toString());
        }

        DescribeServiceIntegrationRequest describeServiceIntegrationRequest =
                Translator.translateToReadServiceIntegrationRequest(model);
        DescribeServiceIntegrationResponse describeServiceIntegrationResponse =
                describeServiceIntegration(describeServiceIntegrationRequest, proxyClient);
        if (describeServiceIntegrationResponse.serviceIntegration().logsAnomalyDetection() == null ||
        OptInStatusConstant.DISABLED.equals(describeServiceIntegrationResponse.serviceIntegration().logsAnomalyDetection().optInStatusAsString())) {
            logger.log(String.format("[READ] LogAnomalyDetection is already DISABLED/null for account %s, will throw CfnNotFoundException",
                            accountId));
            throw new CfnNotFoundException(model.TYPE_NAME, model.getPrimaryIdentifier().toString());
        }

        model.setAccountId(accountId);

        return  ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(model)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
