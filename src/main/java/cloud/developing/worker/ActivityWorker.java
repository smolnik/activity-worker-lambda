package cloud.developing.worker;

import static java.lang.String.format;
import static java.lang.System.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;

public class ActivityWorker {

	public void run(Context context) throws Exception {
		LambdaLogger logger = context.getLogger();
		String accountNumber = context.getInvokedFunctionArn().split(":")[4];
		AWSStepFunctions sf = AWSStepFunctionsClientBuilder.defaultClient();
		String region = getenv("AWS_REGION");
		String apiDeploymentId = getenv("API_DEPLOYMENT_ID");
		String activityArn = format("arn:aws:states:%s:%s:activity:manual-approval", region, accountNumber);
		logger.log("activityArn: " + accountNumber);
		GetActivityTaskResult result = sf.getActivityTask(new GetActivityTaskRequest().withActivityArn(activityArn));
		String taskToken = result.getTaskToken();
		if (taskToken == null) {
			logger.log("No task found.");
			return;
		}
		String encodedTaskToken = encode(taskToken);
		String name = "John Doe";
		String baseLink = format("https://%s.execute-api.us-east-1.amazonaws.com/prod/", apiDeploymentId)
				.concat("%s?taskToken=%s");
		String approvalLink = format(baseLink, "succeed", encodedTaskToken);
		String rejectLink = format(baseLink, "fail", encodedTaskToken);
		String message = format(
				"Hi!%n%s has been nominated for promotion!%nCan you please approve:%n%s%nOr reject:%n%s%n", name,
				approvalLink, rejectLink);
		AmazonSNS sns = AmazonSNSClientBuilder.defaultClient();
		String topicArn = format("arn:aws:sns:%s:%s:employee-promotion-process", region, accountNumber);
		logger.log("message: " + message);
		sns.publish(topicArn, message);
	}

	private static String encode(String s) throws Exception {
		return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
	}

}