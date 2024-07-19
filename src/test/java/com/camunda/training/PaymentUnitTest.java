package com.camunda.training;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.camunda.community.process_test_coverage.junit5.platform7.ProcessEngineCoverageExtension;

@ExtendWith(ProcessEngineCoverageExtension.class)
@Deployment(resources = {"Exercise7.bpmn"})
public class PaymentUnitTest {

  @Test
  public void happy_path_test() {
    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("PaymentProcessEx9",
        withVariables("orderTotal", 45.99, "customerCredit", 30.00));
    assertThat(processInstance).isWaitingAt(findId("Deduct customer credit")).externalTask()
        .hasTopicName("creditDeduction");

    complete(externalTask());

    assertThat(processInstance).isWaitingAt(findId("Charge credit card")).externalTask()
        .hasTopicName("creditCardCharging");

    complete(externalTask());


    assertThat(processInstance).isWaitingAt(findId("Payment completed")).externalTask()
    .hasTopicName("paymentCompletion");

    complete(externalTask());
  }
  
  @Test
  public void credit_card_failure_test() {
    ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("PaymentProcessEx9")
        .startBeforeActivity(findId("Charge credit card")).execute();

    assertThat(processInstance).isWaitingAt(findId("Charge credit card"));

    fetchAndLock("creditCardCharging", "junit-test-worker", 1);
    externalTaskService().handleBpmnError(externalTask().getId(), "junit-test-worker",
        "creditCardChargeError");

    assertThat(processInstance).isWaitingAt(findId("Payment failed")).externalTask()
        .hasTopicName("paymentCompletion");

  }


}
