package software.amazon.devopsguru.loganomalydetectionintegration;

import software.amazon.awssdk.services.devopsguru.model.DescribeServiceIntegrationRequest;
import software.amazon.awssdk.services.devopsguru.model.LogsAnomalyDetectionIntegrationConfig;
import software.amazon.awssdk.services.devopsguru.model.UpdateServiceIntegrationConfig;
import software.amazon.awssdk.services.devopsguru.model.UpdateServiceIntegrationRequest;


import static software.amazon.devopsguru.loganomalydetectionintegration.Constants.OptInStatusConstant;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 */
public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to add a resource
   */
  static UpdateServiceIntegrationRequest translateToAddLogAnomalyDetectionIntegrationRequest(final ResourceModel model){


    final LogsAnomalyDetectionIntegrationConfig logsAnomalyDetectionIntegrationConfig
            = constructLogsAnomalyDetectionIntegration(OptInStatusConstant.ENABLED);

    final UpdateServiceIntegrationConfig updateServiceIntegrationConfig = UpdateServiceIntegrationConfig.builder()
            .logsAnomalyDetection(logsAnomalyDetectionIntegrationConfig)
            .build();

    return UpdateServiceIntegrationRequest.builder()
            .serviceIntegration(updateServiceIntegrationConfig)
            .build();
  }

  /**
   * Request to read a resource
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeServiceIntegrationRequest translateToReadServiceIntegrationRequest(final ResourceModel model) {
      return DescribeServiceIntegrationRequest.builder().build();
  }

  /**
   * Request to delete a resource
   * @return UpdateServiceIntegrationRequest the aws service request to delete a resource
   */
  static UpdateServiceIntegrationRequest translateToDeleteServiceIntegrationRequest(final ResourceModel model) {

    final LogsAnomalyDetectionIntegrationConfig logsAnomalyDetectionIntegrationConfig
            = constructLogsAnomalyDetectionIntegration(OptInStatusConstant.DISABLED);

    final UpdateServiceIntegrationConfig updateServiceIntegrationConfig = UpdateServiceIntegrationConfig.builder()
            .logsAnomalyDetection(logsAnomalyDetectionIntegrationConfig)
            .build();

    return UpdateServiceIntegrationRequest.builder()
            .serviceIntegration(updateServiceIntegrationConfig)
            .build();
  }


  static LogsAnomalyDetectionIntegrationConfig constructLogsAnomalyDetectionIntegration(String optInStatus){

    return LogsAnomalyDetectionIntegrationConfig.builder()
            .optInStatus(optInStatus)
            .build();
  }
}
