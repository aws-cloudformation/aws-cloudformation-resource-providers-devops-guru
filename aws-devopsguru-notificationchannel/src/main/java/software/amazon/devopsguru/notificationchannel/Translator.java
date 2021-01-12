package software.amazon.devopsguru.notificationchannel;

import software.amazon.awssdk.services.devopsguru.model.AddNotificationChannelRequest;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsRequest;
import software.amazon.awssdk.services.devopsguru.model.NotificationChannelConfig;
import software.amazon.awssdk.services.devopsguru.model.RemoveNotificationChannelRequest;
import software.amazon.awssdk.services.devopsguru.model.SnsChannelConfig;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static AddNotificationChannelRequest translateToAddNotificationChannelRequest(final ResourceModel model) {
    if(model.getConfig().getSns().getTopicArn() == null){
      throw new CfnInvalidRequestException(String.format("Could not find SNS TopicArn in %s", model.toString()));
    }

    return AddNotificationChannelRequest.builder()
            .config(configFromModel(model))
            .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static RemoveNotificationChannelRequest translateToRemoveNotificationChannelRequest(final ResourceModel model) {
    return RemoveNotificationChannelRequest.builder()
            .id(model.getId())
            .build();
  }

  /**
   * Request to list resources
   * @param model resource model
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListNotificationChannelsRequest translateToListNotificationChannelRequest(final ResourceModel model) {
    return ListNotificationChannelsRequest.builder()
            .build();
  }

  static ResourceModel translateFromReadResponse(final String topicArn, final String id) {

    software.amazon.devopsguru.notificationchannel.SnsChannelConfig sns =
            software.amazon.devopsguru.notificationchannel.SnsChannelConfig.builder().topicArn(topicArn).build();

    software.amazon.devopsguru.notificationchannel.NotificationChannelConfig config =
    software.amazon.devopsguru.notificationchannel.NotificationChannelConfig.builder().sns(sns).build();
    return ResourceModel.builder()
            .config(config)
            .id(id)
            .build();

  }

  private static NotificationChannelConfig configFromModel(final ResourceModel model) {
    String topicArn = model.getConfig().getSns().getTopicArn();

    return NotificationChannelConfig.builder().sns(SnsChannelConfig.builder().topicArn(topicArn).build()).build();
  }
}
