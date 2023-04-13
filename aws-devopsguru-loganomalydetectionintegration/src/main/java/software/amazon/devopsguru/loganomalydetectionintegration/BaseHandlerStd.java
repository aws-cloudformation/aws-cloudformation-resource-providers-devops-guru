package software.amazon.devopsguru.loganomalydetectionintegration;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.TagLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.TagLogGroupResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.UntagLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.UntagLogGroupResponse;
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationResponse;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ResourceNotFoundException;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.UpdateServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.UpdateServiceIntegrationResponse;
import software.amazon.awssdk.services.devopsguru.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.HashMap;
import java.util.Map;

// Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers

public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

  public CloudWatchLogsClient cloudWatchLogsClient;
  public ProxyClient<CloudWatchLogsClient> cloudWatchLogsProxyClient;

  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
          final AmazonWebServicesClientProxy proxy,
          final ResourceHandlerRequest<ResourceModel> request,
          final CallbackContext callbackContext,
          final Logger logger) {
    return handleRequest(
            proxy,
            request,
            callbackContext != null ? callbackContext : new CallbackContext(),
            proxy.newProxy(ClientBuilder::getDevopsGuruClient),
            logger
    );
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
          final AmazonWebServicesClientProxy proxy,
          final ResourceHandlerRequest<ResourceModel> request,
          final CallbackContext callbackContext,
          final ProxyClient<DevOpsGuruClient> proxyClient,
          final Logger logger);

  protected UpdateServiceIntegrationResponse updateServiceIntegration(
          final UpdateServiceIntegrationRequest updateServiceIntegrationRequest,
          final ProxyClient<DevOpsGuruClient> proxyClient
          ) {

    UpdateServiceIntegrationResponse awsResponse = null;

    try {
      awsResponse = proxyClient.injectCredentialsAndInvokeV2(updateServiceIntegrationRequest, proxyClient.client()::updateServiceIntegration);
    } catch (final AccessDeniedException e) {
      throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
    } catch (final InternalServerException e) {
      throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
    } catch (final ValidationException e) {
      throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
    } catch (final ThrottlingException e) {
      throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
    }catch (final ResourceNotFoundException e) {
      throw new CfnNotFoundException(ResourceModel.TYPE_NAME, ResourceModel.IDENTIFIER_KEY_ACCOUNTID, e);
    }
    return awsResponse;
  }

  protected DescribeServiceIntegrationResponse describeServiceIntegration(
          final DescribeServiceIntegrationRequest describeServiceIntegrationRequest,
          final ProxyClient<DevOpsGuruClient> proxyClient) {

    DescribeServiceIntegrationResponse awsResponse = null;

    try {
      awsResponse = proxyClient.injectCredentialsAndInvokeV2(describeServiceIntegrationRequest,
              proxyClient.client()::describeServiceIntegration);
    } catch (final AccessDeniedException e) {
      throw new CfnAccessDeniedException(ResourceModel.TYPE_NAME, e);
    } catch (final InternalServerException e) {
      throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME, e);
    } catch (final ValidationException e) {
      throw new CfnInvalidRequestException(ResourceModel.TYPE_NAME, e);
    } catch (final ThrottlingException e) {
      throw new CfnThrottlingException(ResourceModel.TYPE_NAME, e);
    } catch (final ResourceNotFoundException e) {
      throw new CfnNotFoundException(ResourceModel.TYPE_NAME, ResourceModel.IDENTIFIER_KEY_ACCOUNTID,e);
    }

    return awsResponse;
  }


  protected void createCloudWatchLogsProxyClient(AmazonWebServicesClientProxy proxy, CloudWatchLogsClient cloudWatchLogsClient) {

    if (cloudWatchLogsClient == null){
      this.cloudWatchLogsClient = ClientBuilder.getCloudWatchClient();
      this.cloudWatchLogsProxyClient = proxy.newProxy(ClientBuilder::getCloudWatchClient);
    }
  }

  //  permissions check used in create/update/delete handlers

  protected ProgressEvent<ResourceModel, CallbackContext> checkForTagLogGroupPermissions(
          final ProgressEvent<ResourceModel, CallbackContext> progressEvent,
          final AmazonWebServicesClientProxy proxy,
          final ProxyClient<CloudWatchLogsClient> cloudWatchLogsClientProxyClient
  ) {

    final ResourceModel model = progressEvent.getResourceModel();
    final CallbackContext callbackContext = progressEvent.getCallbackContext();

    boolean hasPermissions = true;


    Map<String,String> tagMap = new HashMap<>();
    tagMap.put("remove","soon");

    final TagLogGroupRequest tagLogGroupRequest = TagLogGroupRequest.builder()
            .logGroupName("devops-guru-log-anomaly-enablement-permissions-check")
            .tags(tagMap)
            .build();

    final UntagLogGroupRequest untagLogGroupRequest = UntagLogGroupRequest.builder()
            .logGroupName("devops-guru-log-anomaly-enablement-permissions-check")
            .tags("remove")
            .build();

    try {
      TagLogGroupResponse tagLogGroupResponse = cloudWatchLogsClientProxyClient.injectCredentialsAndInvokeV2(tagLogGroupRequest,
              cloudWatchLogsClientProxyClient.client()::tagLogGroup);
    } catch (AwsServiceException e) {
      if (!(e instanceof software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException)) {
        hasPermissions = false;
      }
    }

    try {
      UntagLogGroupResponse untagLogGroupResponse =  cloudWatchLogsClientProxyClient.injectCredentialsAndInvokeV2(untagLogGroupRequest,
              cloudWatchLogsClientProxyClient.client()::untagLogGroup);
    } catch (AwsServiceException e) {
      if (!(e instanceof software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException)) {
        hasPermissions = false;
      }
    }

    if (hasPermissions){
      return ProgressEvent.progress(model, callbackContext);
    } else{
      throw new CfnAccessDeniedException(String.format("User is not authorized to perform logs:TagResource or logs:UntagResource."));
    }
  }
}
