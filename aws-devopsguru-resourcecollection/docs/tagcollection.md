# AWS::DevOpsGuru::ResourceCollection TagCollection

Tagged resource for DevOps Guru to monitor

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#appboundarykey" title="AppBoundaryKey">AppBoundaryKey</a>" : <i>String</i>,
    "<a href="#tagvalues" title="TagValues">TagValues</a>" : <i>[ String, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#appboundarykey" title="AppBoundaryKey">AppBoundaryKey</a>: <i>String</i>
<a href="#tagvalues" title="TagValues">TagValues</a>: <i>
      - String</i>
</pre>

## Properties

#### AppBoundaryKey

A Tag key for DevOps Guru app boundary.

_Required_: No

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>128</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### TagValues

Tag values of DevOps Guru app boundary.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
