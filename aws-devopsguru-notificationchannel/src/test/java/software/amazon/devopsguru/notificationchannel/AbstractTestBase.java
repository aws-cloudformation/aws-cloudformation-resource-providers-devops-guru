package software.amazon.devopsguru.notificationchannel;

import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.provider.Arguments;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.awssdk.services.devopsguru.model.NotificationChannel;
import software.amazon.awssdk.services.devopsguru.model.NotificationChannelConfig;
import software.amazon.awssdk.services.devopsguru.model.NotificationFilterConfig;
import software.amazon.awssdk.services.devopsguru.model.SnsChannelConfig;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Credentials;
import software.amazon.cloudformation.proxy.LoggerProxy;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractTestBase {
  protected static final Credentials MOCK_CREDENTIALS;
  protected static final LoggerProxy logger;

  static final String id = "f3735c1c-dba4-450f-b2e7-030f2acee0b6";
  static final String id1 = "a3735c1c-dba4-450f-b2e7-030f2acee0b6";
  static final String topicArn = "arn:aws:sns:us-east-1:123456789012:DefaultNotificationChannel";
  static final String topicArn1 = "arn:aws:sns:us-east-1:123456789012:DefaultNotificationChannel1";
  static final List<String> insightSeveritiesFilter = Arrays.asList("LOW", "MEDIUM");
  static final List<String> messageTypesFilter = Arrays.asList("CLOSED_INSIGHT", "NEW_ASSOCIATION");

  static {
    MOCK_CREDENTIALS = new Credentials("accessKey", "secretKey", "token");
    logger = new LoggerProxy();
  }
  static ProxyClient<DevOpsGuruClient> MOCK_PROXY(
    final AmazonWebServicesClientProxy proxy,
    final DevOpsGuruClient sdkClient) {
    return new ProxyClient<DevOpsGuruClient>() {
      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseT
      injectCredentialsAndInvokeV2(RequestT request, Function<RequestT, ResponseT> requestFunction) {
        return proxy.injectCredentialsAndInvokeV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse>
      CompletableFuture<ResponseT>
      injectCredentialsAndInvokeV2Async(RequestT request, Function<RequestT, CompletableFuture<ResponseT>> requestFunction) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse, IterableT extends SdkIterable<ResponseT>>
      IterableT
      injectCredentialsAndInvokeIterableV2(RequestT request, Function<RequestT, IterableT> requestFunction) {
        return proxy.injectCredentialsAndInvokeIterableV2(request, requestFunction);
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseInputStream<ResponseT>
      injectCredentialsAndInvokeV2InputStream(RequestT requestT, Function<RequestT, ResponseInputStream<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public <RequestT extends AwsRequest, ResponseT extends AwsResponse> ResponseBytes<ResponseT>
      injectCredentialsAndInvokeV2Bytes(RequestT requestT, Function<RequestT, ResponseBytes<ResponseT>> function) {
        throw new UnsupportedOperationException();
      }

      @Override
      public DevOpsGuruClient client() {
        return sdkClient;
      }
    };
  }

  void assertResponse(ProgressEvent<ResourceModel, CallbackContext> response, ResourceModel responseModel) {
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(response.getCallbackContext()).isNull();
    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
    assertThat(response.getResourceModel()).isEqualTo(responseModel);
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getMessage()).isNull();
    assertThat(response.getErrorCode()).isNull();
  }

  // used for ListHandlerTest
  static Stream<Arguments> validationNotificationParams() {
    return Stream.of(Arguments.of(constructNotificationChannel(topicArn, id, null, null)),
            Arguments.of(constructNotificationChannel(topicArn, id, insightSeveritiesFilter, null)),
            Arguments.of(constructNotificationChannel(topicArn, id, null, messageTypesFilter)),
            Arguments.of(constructNotificationChannel(topicArn, id, insightSeveritiesFilter, messageTypesFilter)));
  }

  // used for CreateHandlerTest, ReadHandlerTest and DeleteHandlerTest
  static Stream<Arguments> validationNotificationChannelWithResourceModelParams() {
    NotificationChannel secondChannel = constructNotificationChannel(topicArn1, id1, insightSeveritiesFilter, messageTypesFilter);
    Pair<List<NotificationChannel>, ResourceModel> pair1 = Pair.of(
            Arrays.asList(constructNotificationChannel(topicArn, id, null, null), secondChannel),
            constructResourceModel(topicArn, id, null, null));
    Pair<List<NotificationChannel>, ResourceModel> pair2 =Pair.of(
            Arrays.asList(constructNotificationChannel(topicArn, id, insightSeveritiesFilter, null), secondChannel),
            constructResourceModel(topicArn, id, insightSeveritiesFilter, null));
    Pair<List<NotificationChannel>, ResourceModel> pair3 = Pair.of(
            Arrays.asList(constructNotificationChannel(topicArn, id, null, messageTypesFilter), secondChannel),
            constructResourceModel(topicArn, id, null, messageTypesFilter));
    Pair<List<NotificationChannel>, ResourceModel> pair4 = Pair.of(
            Arrays.asList(constructNotificationChannel(topicArn, id, insightSeveritiesFilter, messageTypesFilter), secondChannel),
            constructResourceModel(topicArn, id, insightSeveritiesFilter, messageTypesFilter));

    return Stream.of(
            Arguments.of(pair1), Arguments.of(pair2), Arguments.of(pair3), Arguments.of(pair4));
  }


  static NotificationChannel constructNotificationChannel(String topicArn,
                                                           String id,
                                                           List<String> insightSeverities,
                                                           List<String> messageTypes) {

    final SnsChannelConfig sns = SnsChannelConfig.builder().topicArn(topicArn).build();
    final NotificationChannelConfig config;
    if (insightSeverities == null && messageTypes == null) {
      config = NotificationChannelConfig.builder().sns(sns).build();
    } else {
      config = NotificationChannelConfig.builder().sns(sns)
              .filters(NotificationFilterConfig.builder()
                      .severitiesWithStrings(insightSeverities)
                      .messageTypesWithStrings(messageTypes)
                      .build())
              .build();
    }
    return NotificationChannel.builder().config(config).id(id).build();
  }

  static ResourceModel constructResourceModel(String topicArn,
                                              String id,
                                              List<String> insightSeverities,
                                              List<String> messageTypes) {
    return ResourceModel.builder()
            .config(new software.amazon.devopsguru.notificationchannel.NotificationChannelConfig(
                    new software.amazon.devopsguru.notificationchannel.SnsChannelConfig(topicArn),
                    new software.amazon.devopsguru.notificationchannel.NotificationFilterConfig(insightSeverities, messageTypes)))
            .id(id)
            .build();
  }
}
