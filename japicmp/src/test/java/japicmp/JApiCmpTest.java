package japicmp;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.Assertion;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.contrib.java.lang.system.LogMode;

import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class JApiCmpTest {
	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();
	@Rule
	public final ErrLogRule errLog = new ErrLogRule(LogMode.LOG_ONLY);
	@Rule
	public final OutLogRule outLog = new OutLogRule(LogMode.LOG_ONLY);

	@Test
	public void testWithoutArguments() {
		exit.expectSystemExitWithStatus(1);
		exit.checkAssertionAfterwards(new Assertion() {
			public void checkAssertion() {
				assertThat(errLog.getLog().trim(), containsString("E: Required option".trim()));
				assertThatUseHelpOptionIsPrinted();
			}
		});
		JApiCmp.mainVar();
	}

	private void assertThatUseHelpOptionIsPrinted() {
		assertThat(outLog.getLog(), containsString(JApiCmp.USE_HELP_OR_H_FOR_MORE_INFORMATION));
	}

	private void assertThatHelpIsPrinted() {
		assertThat(outLog.getLog(), containsString("NAME"));
		assertThat(outLog.getLog(), containsString("SYNOPSIS"));
		assertThat(outLog.getLog(), containsString("OPTIONS"));
	}

	@Test
	public void testHelp() {
		exit.checkAssertionAfterwards(new Assertion() {
			public void checkAssertion() {
				assertThat(errLog.getLog().trim(), not(containsString("E: ".trim())));
				assertThatHelpIsPrinted();
			}
		});
		JApiCmp.mainVar("-h");
	}

	@Test
	public void testHelpLongOption() {
		exit.checkAssertionAfterwards(new Assertion() {
			public void checkAssertion() {
				assertThat(errLog.getLog().trim(), not(containsString("E: ".trim())));
				assertThatHelpIsPrinted();
			}
		});
		JApiCmp.mainVar("--help");
	}

	@Test
	public void testWithNewArchiveOptionButWithoutArgument() {
		exit.expectSystemExitWithStatus(128);
		exit.checkAssertionAfterwards(new Assertion() {
			public void checkAssertion() {
				assertThat(errLog.getLog().trim(), containsString("E: Required values for option 'pathToNewVersionJar' not provided".trim()));
				assertThatUseHelpOptionIsPrinted();
			}
		});
		JApiCmp.mainVar("-n");
	}

	@Test
	public void testWithOldArchiveOptionButWithoutArgument() {
		exit.expectSystemExitWithStatus(128);
		exit.checkAssertionAfterwards(new Assertion() {
			public void checkAssertion() {
				assertThat(errLog.getLog().trim(), containsString("E: Required values for option 'pathToOldVersionJar' not provided".trim()));
				assertThatUseHelpOptionIsPrinted();
			}
		});
		JApiCmp.mainVar("-o");
	}

	@Test
	public void testWithNewArchiveOptionButWithInvalidArgument() {
		exit.expectSystemExitWithStatus(1);
		exit.checkAssertionAfterwards(new Assertion() {
			public void checkAssertion() {
				String errLogTrimmed = errLog.getLog().trim();
				assertThat(errLogTrimmed, containsString("E: File".trim()));
				assertThat(errLogTrimmed, containsString("does not exist.".trim()));
				assertThatUseHelpOptionIsPrinted();
			}
		});
		JApiCmp.mainVar("-n", "xyz.jar", "-o", "zyx.jar");
	}

	@Test
	public void testWithOldArchiveOptionButWithInvalidArgument() {
		exit.expectSystemExitWithStatus(1);
		exit.checkAssertionAfterwards(new Assertion() {
			public void checkAssertion() {
				String errLogTrimmed = errLog.getLog().trim();
				assertThat(errLogTrimmed, containsString("E: File".trim()));
				assertThat(errLogTrimmed, containsString("does not exist.".trim()));
				assertThatUseHelpOptionIsPrinted();
			}
		});
		JApiCmp.mainVar("-n", pathTo("new.jar"), "-o", "xyz.jar");
	}

	@Test
	public void testWithOldArchiveOptionAndNewArchiveOption() {
		exit.checkAssertionAfterwards(new Assertion() {
			public void checkAssertion() {
				assertThat(errLog.getLog().trim(), not(containsString("E: ".trim())));
			}
		});
		JApiCmp.mainVar("-n", pathTo("new.jar"), "-o", pathTo("old.jar"));
	}

	private String pathTo(String jarFileName) {
		return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", jarFileName).toString();
	}

	static void assertListsEquals(ImmutableList<String> expected, ImmutableList<String> actual) {
		Joiner nlJoiner = Joiner.on("\n");
		assertEquals(nlJoiner.join(expected), nlJoiner.join(actual));
	}
}
