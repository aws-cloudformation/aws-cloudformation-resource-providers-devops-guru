package software.amazon.devopsguru.notificationchannel;

import software.amazon.awssdk.services.devopsguru.model.AddNotificationChannelRequest;
import software.amazon.awssdk.services.devopsguru.model.ListNotificationChannelsRequest;
import software.amazon.awssdk.services.devopsguru.model.NotificationChannelConfig;
import software.amazon.awssdk.services.devopsguru.model.RemoveNotificationChannelRequest;
import software.amazon.awssdk.services.devopsguru.model.SnsChannelConfig;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;

import java.util.List;

import static software.amazon.devopsguru.notificationchannel.constants.FilterConstants.INSIGHT_SEVERITIES;
import static software.amazon.devopsguru.notificationchannel.constants.FilterConstants.MESSAGE_TYPES;


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

  static ResourceModel translateFromReadResponse(final String topicArn, final String id,
                                                 List<String> severities,
                                                 List<String> messageTypes) {

    final software.amazon.devopsguru.notificationchannel.SnsChannelConfig sns =
            software.amazon.devopsguru.notificationchannel.SnsChannelConfig.builder().topicArn(topicArn).build();

    final NotificationFilterConfig filters = NotificationFilterConfig.builder()
            .severities(severities)
            .messageTypes(messageTypes)
            .build();

    final software.amazon.devopsguru.notificationchannel.NotificationChannelConfig config =
    software.amazon.devopsguru.notificationchannel.NotificationChannelConfig.builder()
            .sns(sns)
            .filters(filters)
            .build();
    return ResourceModel.builder()
            .config(config)
            .id(id)
            .build();
  }

  private static NotificationChannelConfig configFromModel(final ResourceModel model) {
    final String topicArn = model.getConfig().getSns().getTopicArn();
    final NotificationFilterConfig notificationFilterConfig = model.getConfig().getFilters();

    final software.amazon.awssdk.services.devopsguru.model.NotificationFilterConfig filters =
            software.amazon.awssdk.services.devopsguru.model.NotificationFilterConfig.builder()
                    .severitiesWithStrings(translateToNotificationFiltersFromModel(INSIGHT_SEVERITIES, notificationFilterConfig))
                    .messageTypesWithStrings(translateToNotificationFiltersFromModel(MESSAGE_TYPES, notificationFilterConfig))
                    .build();

    return NotificationChannelConfig.builder()
            .sns(SnsChannelConfig.builder().topicArn(topicArn).build())
            .filters(filters)
            .build();
  }

  private static List<String> translateToNotificationFiltersFromModel(final String fieldName,
                                                                      final NotificationFilterConfig notificationFilterConfig) {

    if (notificationFilterConfig == null) {
      return null;
    }

    if (INSIGHT_SEVERITIES.equals(fieldName)) {
      return notificationFilterConfig.getSeverities();
    }
    return notificationFilterConfig.getMessageTypes();
  }
}
