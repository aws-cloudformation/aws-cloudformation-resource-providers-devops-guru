package software.amazon.devopsguru.loganomalydetectionintegration;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {

    public static DevOpsGuruClient getDevopsGuruClient() {
        return DevOpsGuruClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }

    public static CloudWatchLogsClient getCloudWatchClient() {
        return CloudWatchLogsClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
