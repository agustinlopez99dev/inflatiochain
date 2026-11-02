package dev.agustinlopez.inflatiochain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import dev.agustinlopez.inflatiochain.service.PrewarmJob;

/** Guards the default configuration: the app must start with the prewarm job enabled, as it does for real users. */
@SpringBootTest(properties = {
		"inflatiochain.prewarm.enabled=true",
		"inflatiochain.prewarm.initial-delay=1h"
})
class PrewarmWiringTest {

	@Autowired ApplicationContext context;

	@Test
	void contextStartsWithPrewarmJobEnabled() {
		assertThat(context.getBean(PrewarmJob.class)).isNotNull();
	}
}
