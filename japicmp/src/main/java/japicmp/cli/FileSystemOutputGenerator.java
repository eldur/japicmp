package japicmp.cli;

import japicmp.config.Options;
import japicmp.exception.JApiCmpException;
import japicmp.model.JApiClass;
import japicmp.output.OutputGenerator;
import japicmp.output.semver.SemverOut;
import japicmp.output.xml.XmlOutput;
import japicmp.output.xml.XmlOutputGenerator;
import japicmp.output.xml.XmlOutputGeneratorOptions;

import java.util.List;

public class FileSystemOutputGenerator extends OutputGenerator<Void> {
	public FileSystemOutputGenerator(Options options, List<JApiClass> jApiClasses) {
		super(options, jApiClasses);
	}

	@Override
	public Void generate() {
		SemverOut semverOut = new SemverOut(options, jApiClasses);
		XmlOutputGeneratorOptions xmlOutputGeneratorOptions = new XmlOutputGeneratorOptions();
		xmlOutputGeneratorOptions.setCreateSchemaFile(true);
		xmlOutputGeneratorOptions.setSemanticVersioningInformation(semverOut.generate());
		XmlOutputGenerator xmlGenerator = new XmlOutputGenerator(jApiClasses, options, xmlOutputGeneratorOptions);
		try (XmlOutput xmlOutput = xmlGenerator.generate()) {
			XmlOutputGenerator.writeToFiles(xmlOutput, options.getXmlOutputFile(), options.getHtmlOutputFile());
		} catch (Exception e) {
			throw new JApiCmpException(JApiCmpException.Reason.IoException, "Could not close output streams: " + e.getMessage(), e);
		}
		return null;
	}
}
