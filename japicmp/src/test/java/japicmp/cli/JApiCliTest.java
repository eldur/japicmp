package japicmp.cli;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import japicmp.OutLogRule;
import japicmp.config.Options;
import japicmp.model.JApiClass;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.LogMode;

import java.util.List;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class JApiCliTest {

	@Rule
	public final OutLogRule outLog = new OutLogRule(LogMode.LOG_ONLY);

	static Matcher<String> equalsIgnoreNewline(final String expectedValue) {
		return new BaseMatcher<String>() {
			@Override
			public void describeTo(Description description) {
				description.appendValue(expectedValue);
			}

			@Override
			public boolean matches(Object actualValue) {
				boolean identical = expectedValue.equals(actualValue);
				if (!identical) {
					return expectedValue.equals(replace((String) actualValue));
				} else {
					return false;
				}
			}

			@Override
			public void describeMismatch(Object item, Description description) {
				description.appendText("was ").appendValue(replace((String) item));
			}

			private String replace(String actualValue) {
				return actualValue.trim().replaceAll("\r\n", "\n");
			}
		};
	}

	@Test
	public void testGenerateOutput() {
		// GIVEN
		JApiCli.Compare testee = new JApiCli.Compare();
		Options options = Options.newDefault();
		List<JApiClass> classes = ImmutableList.of();

		// WHEN
		testee.generateOutput(options, classes, mock(FileSystemOutputGenerator.class));

		// THEN
		assertThat(outLog.getLog(), equalsIgnoreNewline("Comparing  with :\nNo changes."));
	}

	@Test
	public void testGenerateOutput_semver() {
		// GIVEN
		JApiCli.Compare testee = new JApiCli.Compare();
		Options options = Options.newDefault();
		testee.semanticVersioningOnly = true;
		List<JApiClass> classes = ImmutableList.of();

		// WHEN
		testee.generateOutput(options, classes, mock(FileSystemOutputGenerator.class));

		// THEN
		assertThat(outLog.getLog(), equalsIgnoreNewline("0.0.1"));
	}

	@Test
	public void testGenerateOutput_html() {
		// GIVEN
		JApiCli.Compare testee = new JApiCli.Compare();
		Options options = Options.newDefault();
		options.setHtmlOutputFile(Optional.of("any.html"));
		List<JApiClass> classes = ImmutableList.of();

		// WHEN
		testee.generateOutput(options, classes, mock(FileSystemOutputGenerator.class));

		// THEN
		assertThat(outLog.getLog(), equalsIgnoreNewline("Comparing  with :\nNo changes."));
	}

}
