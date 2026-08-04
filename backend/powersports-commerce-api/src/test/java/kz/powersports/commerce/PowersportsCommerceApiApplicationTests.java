package kz.powersports.commerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"woocommerce.consumer-key=test-consumer-key",
		"woocommerce.consumer-secret=test-consumer-secret",
		"woocommerce.webhook-secret=test-webhook-secret"
})
class PowersportsCommerceApiApplicationTests {

	@Test
	void contextLoads() {
	}
}
