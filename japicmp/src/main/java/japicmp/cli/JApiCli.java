package japicmp.cli;

import javax.inject.Inject;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import io.airlift.airline.Command;
import io.airlift.airline.HelpOption;
import io.airlift.airline.Option;
import japicmp.cmp.JarArchiveComparator;
import japicmp.cmp.JarArchiveComparatorOptions;
import japicmp.config.Options;
import japicmp.exception.JApiCmpException;
import japicmp.model.AccessModifier;
import japicmp.model.JApiClass;
import japicmp.output.semver.SemverOut;
import japicmp.output.stdout.StdoutOutputGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JApiCli {
	public static final String IGNORE_MISSING_CLASSES = "--ignore-missing-classes";
	public static final String OLD_CLASSPATH = "--old-classpath";
	public static final String NEW_CLASSPATH = "--new-classpath";

	public enum ClassPathMode {
		ONE_COMMON_CLASSPATH, TWO_SEPARATE_CLASSPATHS
	}

	@Command(name = "java -jar japicmp.jar", description = "Compares jars")
	public static class Compare implements Runnable {
		@Inject
		public HelpOption helpOption;
		@Option(name = { "-o", "--old" }, description = "Provides the path to the old version(s) of the jar(s). Use ; to separate jar files.")
		public String pathToOldVersionJar;
		@Option(name = { "-n", "--new" }, description = "Provides the path to the new version(s) of the jar(s). Use ; to separate jar files.")
		public String pathToNewVersionJar;
		@Option(name = { "-m", "--only-modified" }, description = "Outputs only modified classes/methods.")
		public boolean modifiedOnly;
		@Option(name = { "-b", "--only-incompatible" }, description = "Outputs only classes/methods that are binary incompatible. If not given, all classes and methods are printed.")
		public boolean onlyBinaryIncompatibleModifications;
		@Option(name = "-a", description = "Sets the access modifier level (public, package, protected, private), which should be used.")
		public String accessModifier;
		@Option(name = { "-i", "--include" },
			description = "Semicolon separated list of elements to include in the form package.Class#classMember, * can be used as wildcard. Annotations are given as FQN starting with @. Examples: mypackage;my.Class;other.Class#method(int,long);foo.Class#field;@my.Annotation.")
		public String includes;
		@Option(name = { "-e", "--exclude" },
			description = "Semicolon separated list of elements to exclude in the form package.Class#classMember, * can be used as wildcard. Annotations are given as FQN starting with @. Examples: mypackage;my.Class;other.Class#method(int,long);foo.Class#field;@my.Annotation.")
		public String excludes;
		@Option(name = { "-x", "--xml-file" }, description = "Provides the path to the xml output file.")
		public String pathToXmlOutputFile;
		@Option(name = { "--html-file" }, description = "Provides the path to the html output file.")
		public String pathToHtmlOutputFile;
		@Option(name = { "-s", "--semantic-versioning" }, description = "Tells you which part of the version to increment.")
		public boolean semanticVersioningOnly = false;
		@Option(name = { "--include-synthetic" }, description = "Include synthetic classes and class members that are hidden per default.")
		public boolean includeSynthetic = false;
		@Option(name = { IGNORE_MISSING_CLASSES }, description = "Ignores superclasses/interfaces missing on the classpath.")
		public boolean ignoreMissingClasses = false;
		@Option(name = { "--html-stylesheet" }, description = "Provides the path to your own stylesheet.")
		public String pathToHtmlStylesheet;
		@Option(name = { OLD_CLASSPATH }, description = "The classpath for the old version.")
		public String oldClassPath;
		@Option(name = { NEW_CLASSPATH }, description = "The classpath for the new version.")
		public String newClassPath;
		@Option(name = "--no-annotations", description = "Do not evaluate annotations.")
		public boolean noAnnotations = false;

		@Override
		public void run() {
			Options options = createOptionsFromCliArgs();
			Options.verify(options);
			JarArchiveComparator jarArchiveComparator = new JarArchiveComparator(JarArchiveComparatorOptions.of(options));
			List<JApiClass> jApiClasses = jarArchiveComparator.compare(options.getOldArchives(), options.getNewArchives());
			generateOutput(options, jApiClasses, new FileSystemOutputGenerator(options, jApiClasses));
		}

		@VisibleForTesting
		void generateOutput(Options options, List<JApiClass> jApiClasses, FileSystemOutputGenerator fileSystemOutputGenerator) {
			if (semanticVersioningOnly) {
				SemverOut semverOut = new SemverOut(options, jApiClasses);
				String semverIncrement = semverOut.generate();
				System.out.println(semverIncrement);
			} else {
				Optional<String> xmlOutputFile = options.getXmlOutputFile();
				Optional<String> htmlOutputFile = options.getHtmlOutputFile();
				if (xmlOutputFile.isPresent() || htmlOutputFile.isPresent()) {
					fileSystemOutputGenerator.generate();
				}
				StdoutOutputGenerator stdoutOutputGenerator = new StdoutOutputGenerator(options, jApiClasses);
				String output = stdoutOutputGenerator.generate();
				System.out.println(output);
			}

		}

		private Options createOptionsFromCliArgs() {
			Options options = Options.newDefault();
			options.getOldArchives().addAll(createFileList(checkNonNull(pathToOldVersionJar, "Required option -o is missing.")));
			options.getNewArchives().addAll(createFileList(checkNonNull(pathToNewVersionJar, "Required option -n is missing.")));
			options.setXmlOutputFile(Optional.fromNullable(pathToXmlOutputFile));
			options.setHtmlOutputFile(Optional.fromNullable(pathToHtmlOutputFile));
			options.setOutputOnlyModifications(modifiedOnly);
			options.setAccessModifier(toModifier(accessModifier));
			options.addIncludeFromArgument(Optional.fromNullable(includes));
			options.addExcludeFromArgument(Optional.fromNullable(excludes));
			options.setOutputOnlyBinaryIncompatibleModifications(onlyBinaryIncompatibleModifications);
			options.setIncludeSynthetic(includeSynthetic);
			options.setIgnoreMissingClasses(ignoreMissingClasses);
			options.setHtmlStylesheet(Optional.fromNullable(pathToHtmlStylesheet));
			options.setOldClassPath(Optional.fromNullable(oldClassPath));
			options.setNewClassPath(Optional.fromNullable(newClassPath));
			options.setNoAnnotations(noAnnotations);
			return options;
		}

		private List<File> createFileList(String option) {
			String[] parts = option.split(";");
			List<File> files = new ArrayList<>(parts.length);
			for (String part : parts) {
				File file = new File(part);
				files.add(file);
			}
			return files;
		}

		private <T> T checkNonNull(T in, String errorMessage) {
			if (in == null) {
				throw new JApiCmpException(JApiCmpException.Reason.CliError, errorMessage);
			} else {
				return in;
			}
		}

		private Optional<AccessModifier> toModifier(String accessModifierArg) {
			Optional<String> stringOptional = Optional.fromNullable(accessModifierArg);
			if (stringOptional.isPresent()) {
				try {
					return Optional.of(AccessModifier.valueOf(stringOptional.get().toUpperCase()));
				} catch (IllegalArgumentException e) {
					throw new JApiCmpException(JApiCmpException.Reason.CliError, String.format("Invalid value for option -a: %s. Possible values are: %s.", accessModifierArg, AccessModifier.listOfAccessModifier()));
				}
			} else {
				return Optional.of(AccessModifier.PROTECTED);
			}
		}
	}
}
