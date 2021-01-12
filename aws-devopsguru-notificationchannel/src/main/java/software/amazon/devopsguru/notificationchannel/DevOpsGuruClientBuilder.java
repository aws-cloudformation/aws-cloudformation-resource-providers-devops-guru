package software.amazon.devopsguru.notificationchannel;

import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.devopsguru.DevOpsGuruClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class DevOpsGuruClientBuilder {

    public static DevOpsGuruClient getClient() {
        return DevOpsGuruClient.builder()
                .httpClient(LambdaWrapper.HTTP_CLIENT)
                .build();
    }
}
