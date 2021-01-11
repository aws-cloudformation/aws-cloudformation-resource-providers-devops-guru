package software.amazon.devopsguru.resourcecollection;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.AccessDeniedException;
import software.amazon.awssdk.services.devopsguru.model.CloudFormationCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.GetResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.InternalServerException;
import software.amazon.awssdk.services.devopsguru.model.ResourceCollectionFilter;
import software.amazon.awssdk.services.devopsguru.model.ThrottlingException;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionRequest;
import software.amazon.awssdk.services.devopsguru.model.UpdateResourceCollectionResponse;
import software.amazon.awssdk.services.devopsguru.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnAccessDeniedException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.CfnThrottlingException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<DevOpsGuruClient> proxyClient;

    @Mock
    DevOpsGuruClient sdkClient;

    private UpdateHandler handler;

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        sdkClient = mock(DevOpsGuruClient.class);
        proxyClient = MOCK_PROXY(proxy, sdkClient);
    }

    @Test
    public void handleRequest_DoNothing() {
        final CloudFormationCollectionFilter cloudFormationCollection = CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName")).build();
        final ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse = GetResourceCollectionResponse.builder().resourceCollection(resourceCollection).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollectionFilter = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(model_resourceCollectionFilter)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(0)).updateResourceCollection(any(UpdateResourceCollectionRequest.class));
    }

    @Test
    public void handleRequest_OnlyAdd() {
        final CloudFormationCollectionFilter cloudFormationCollection = CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("A")).build();
        final ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse = GetResourceCollectionResponse.builder().resourceCollection(resourceCollection).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final UpdateResourceCollectionResponse updateResourceCollectionResponse = UpdateResourceCollectionResponse.builder().build();
        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenReturn(updateResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("A", "StackName")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollection = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(model_resourceCollection)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).updateResourceCollection(any(UpdateResourceCollectionRequest.class));
    }

    @Test
    public void handleRequest_UpdateStackNameIsEmpty() {
        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList()).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollection = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(model_resourceCollection)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThatExceptionOfType(CfnInvalidRequestException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_BothAddAndRemove() {
        final CloudFormationCollectionFilter cloudFormationCollection = CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName")).build();
        final ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse = GetResourceCollectionResponse.builder().resourceCollection(resourceCollection).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final UpdateResourceCollectionResponse updateResourceCollectionResponse = UpdateResourceCollectionResponse.builder().build();
        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenReturn(updateResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName1")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollection = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(model_resourceCollection)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(2)).updateResourceCollection(any(UpdateResourceCollectionRequest.class));
    }

    @Test
    public void handleRequest_UpdateStackNameSizeGreaterThanOneBatch() {
        final CloudFormationCollectionFilter cloudFormationCollection = CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("1")).build();
        final ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse = GetResourceCollectionResponse.builder().resourceCollection(resourceCollection).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final UpdateResourceCollectionResponse updateResourceCollectionResponse = UpdateResourceCollectionResponse.builder().build();
        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenReturn(updateResourceCollectionResponse);

        List<String> stringList = new ArrayList<>(102);

        for(int i = 0; i <102; i++){
            stringList.add(String.valueOf(i));
        }

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(stringList).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollection = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(model_resourceCollection)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(2)).updateResourceCollection(any(UpdateResourceCollectionRequest.class));
    }

    @Test
    public void handleRequest_UpdateStackNameIsStar() {
        final CloudFormationCollectionFilter cloudFormationCollection = CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("A")).build();
        final ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse = GetResourceCollectionResponse.builder().resourceCollection(resourceCollection).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final UpdateResourceCollectionResponse updateResourceCollectionResponse = UpdateResourceCollectionResponse.builder().build();
        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenReturn(updateResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("*")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollection = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(model_resourceCollection)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).updateResourceCollection(any(UpdateResourceCollectionRequest.class));
    }

    @Test
    public void handleRequest_ExistingStackNameIsStar() {
        final CloudFormationCollectionFilter cloudFormationCollection = CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("*")).build();
        final ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse = GetResourceCollectionResponse.builder().resourceCollection(resourceCollection).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final UpdateResourceCollectionResponse updateResourceCollectionResponse = UpdateResourceCollectionResponse.builder().build();
        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenReturn(updateResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName1")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollection = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(model_resourceCollection)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        verify(proxyClient.client(), times(1)).updateResourceCollection(any(UpdateResourceCollectionRequest.class));
    }

    @Test
    public void handleRequest_ExistingEmpty() {
        final CloudFormationCollectionFilter cloudFormationCollection = CloudFormationCollectionFilter.builder().stackNames(Arrays.asList()).build();
        final ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse = GetResourceCollectionResponse.builder().resourceCollection(resourceCollection).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName1")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollection = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(model_resourceCollection)
                .build();
        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThatExceptionOfType(CfnNotFoundException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_UpdateStackNameHasMoreThanStar() {
        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName1", "*")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollection = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(model_resourceCollection)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        assertThatExceptionOfType(CfnInvalidRequestException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }

    @Test
    public void handleRequest_Exceptions() {
        final CloudFormationCollectionFilter cloudFormationCollection = CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName")).build();
        final ResourceCollectionFilter resourceCollection = ResourceCollectionFilter.builder().cloudFormation(cloudFormationCollection).build();
        final GetResourceCollectionResponse getResourceCollectionResponse = GetResourceCollectionResponse.builder().resourceCollection(resourceCollection).build();
        when(proxyClient.client().getResourceCollection(any(GetResourceCollectionRequest.class))).thenReturn(getResourceCollectionResponse);

        final software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter cloudFormation = software.amazon.devopsguru.resourcecollection.CloudFormationCollectionFilter.builder().stackNames(Arrays.asList("StackName1")).build();
        final software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter model_resourceCollection = software.amazon.devopsguru.resourcecollection.ResourceCollectionFilter.builder().cloudFormation(cloudFormation).build();
        final ResourceModel model = ResourceModel.builder()
                .resourceCollectionFilter(model_resourceCollection)
                .build();

        final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
                .desiredResourceState(model)
                .build();

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(AccessDeniedException.class);
        assertThatExceptionOfType(CfnAccessDeniedException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(ThrottlingException.class);
        assertThatExceptionOfType(CfnThrottlingException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(InternalServerException.class);
        assertThatExceptionOfType(CfnServiceInternalErrorException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));

        when(proxyClient.client().updateResourceCollection(any(UpdateResourceCollectionRequest.class))).thenThrow(ValidationException.class);
        assertThatExceptionOfType(CfnInvalidRequestException.class).isThrownBy(() -> handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger));
    }
}
