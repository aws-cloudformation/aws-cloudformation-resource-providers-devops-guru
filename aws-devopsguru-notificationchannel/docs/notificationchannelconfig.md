# AWS::DevOpsGuru::NotificationChannel NotificationChannelConfig

Information about notification channels you have configured with DevOps Guru.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#sns" title="Sns">Sns</a>" : <i><a href="snschannelconfig.md">SnsChannelConfig</a></i>,
    "<a href="#filters" title="Filters">Filters</a>" : <i><a href="notificationfilterconfig.md">NotificationFilterConfig</a></i>
}
</pre>

### YAML

<pre>
<a href="#sns" title="Sns">Sns</a>: <i><a href="snschannelconfig.md">SnsChannelConfig</a></i>
<a href="#filters" title="Filters">Filters</a>: <i><a href="notificationfilterconfig.md">NotificationFilterConfig</a></i>
</pre>

## Properties

#### Sns

Information about a notification channel configured in DevOps Guru to send notifications when insights are created.

_Required_: No

_Type_: <a href="snschannelconfig.md">SnsChannelConfig</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Filters

Information about filters of a notification channel configured in DevOpsGuru to filter for insights.

_Required_: No

_Type_: <a href="notificationfilterconfig.md">NotificationFilterConfig</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

