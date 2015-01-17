package japicmp.output.semver;

import java.util.List;

import com.google.common.collect.ImmutableSet;
import japicmp.config.Options;
import japicmp.model.AccessModifier;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiAnnotationElement;
import japicmp.model.JApiBinaryCompatibility;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiHasAccessModifier;
import japicmp.model.JApiHasAnnotations;
import japicmp.model.JApiHasChangeStatus;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiModifier;
import japicmp.model.JApiSuperclass;
import japicmp.output.OutputGenerator;

public class SemverOut extends OutputGenerator {

	public SemverOut(Options options, List<JApiClass> jApiClasses) {
		super(options, jApiClasses);
	}

	@Override
	public void generate() {
		System.err.println(value());
	}

	public String value() {
		options.setOutputOnlyModifications(true);
		options.setAccessModifier(AccessModifier.PROTECTED);

		return generate(jApiClasses);
	}

	public String generate(List<JApiClass> jApiClasses) {
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		for (JApiClass jApiClass : jApiClasses) {
			builder.addAll(processClass(jApiClass));
			builder.addAll(processConstructors(jApiClass));
			builder.addAll(processMethods(jApiClass));
			builder.addAll(processAnnotations(jApiClass, jApiClass));
		}
		ImmutableSet<SemverStatus> build = builder.build();
		if (build.contains(SemverStatus.MAJOR)) {
			return "1.0.0";
		} else if (build.contains(SemverStatus.MINOR)) {
			return "0.1.0";
		} else if (build.isEmpty() || build.contains(SemverStatus.PATCH)) {
			return "0.0.1";
		} else {
			return "N/A";
		}
	}

	private ImmutableSet<SemverStatus> processAnnotations(JApiHasAnnotations jApiClass,
			JApiHasAccessModifier accessModifier) {
		List<JApiAnnotation> annotations = jApiClass.getAnnotations();
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		for (JApiAnnotation jApiAnnotation : annotations) {
			builder.add(detectChangeStatus(jApiAnnotation, accessModifier));
			List<JApiAnnotationElement> elements = jApiAnnotation.getElements();
			for (JApiAnnotationElement jApiAnnotationElement : elements) {
				builder.add(detectChangeStatus(jApiAnnotationElement, accessModifier));
			}
			// following false only for documentation
			if (false && "java.lang.Deprecated".equals(jApiAnnotation.getFullyQualifiedName())) {
				if (jApiAnnotation.getChangeStatus().equals(JApiChangeStatus.NEW)) {
					builder.add(SemverStatus.MINOR);
				}
			}
		}
		return builder.build();
	}

	private ImmutableSet<SemverStatus> processConstructors(JApiClass jApiClass) {
		List<JApiConstructor> constructors = jApiClass.getConstructors();
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		for (JApiConstructor jApiConstructor : constructors) {
			builder.add(detectChangeStatus(jApiConstructor, jApiConstructor));
			builder.addAll(processAnnotations(jApiConstructor, jApiConstructor));
		}
		return builder.build();
	}

	private ImmutableSet<SemverStatus> processMethods(JApiClass jApiClass) {
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		List<JApiMethod> methods = jApiClass.getMethods();
		for (JApiMethod jApiMethod : methods) {
			builder.add(detectChangeStatus(jApiMethod, jApiMethod));
			builder.addAll(processAnnotations(jApiMethod, jApiMethod));
		}
		return builder.build();
	}

	private ImmutableSet<SemverStatus> processClass(JApiClass jApiClass) {
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		builder.addAll(processInterfaceChanges(jApiClass));
		builder.add(processSuperclassChanges(jApiClass));
		builder.addAll(processFieldChanges(jApiClass));
		return builder.build();
	}

	static SemverStatus detectChangeStatus(JApiHasChangeStatus hasChangeStatus,
			JApiHasAccessModifier accessModifier) {
		JApiChangeStatus changeStatus = hasChangeStatus.getChangeStatus();
		switch (changeStatus) {
		case UNCHANGED:
			return SemverStatus.PATCH;
		case NEW:
		case REMOVED:
		case MODIFIED:
			if (hasChangeStatus instanceof JApiBinaryCompatibility) {
				JApiBinaryCompatibility binaryCompatibility = (JApiBinaryCompatibility) hasChangeStatus;
				if (binaryCompatibility.isBinaryCompatible()) {
					if (accessModifier != null) {
						return changeStatusFromAccessModifier(accessModifier);
					} else {
						throw new IllegalStateException("access modifier must not be null");
					}
				} else {
					return SemverStatus.MAJOR;
				}
			} else {
				throw new IllegalStateException(
						"change status must implement " + JApiBinaryCompatibility.class.getCanonicalName());
			}
		default:
			throw new IllegalStateException("only " + JApiChangeStatus.knownValues() + " are allowed");
		}
	}

	private ImmutableSet<SemverStatus> processFieldChanges(JApiClass jApiClass) {
		List<JApiField> fields = jApiClass.getFields();
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		for (JApiField field : fields) {
			builder.add(detectChangeStatus(field, field));
			builder.addAll(processAnnotations(field, field));
		}
		return builder.build();
	}

	static SemverStatus changeStatusFromAccessModifier(JApiHasAccessModifier modifier) {

		JApiModifier<AccessModifier> accessModifier = modifier.getAccessModifier();
		if (isOldAndNewPresent(accessModifier)) {
			if (isOldPublic(accessModifier)) {
				if (isNewPublic(accessModifier)) {
					return SemverStatus.PATCH;
				} else {
					return SemverStatus.MAJOR;
				}
			} else if (isNewPublic(accessModifier)) {
				return SemverStatus.MINOR;
			} else {
				return SemverStatus.PATCH;
			}
		} else if (ifOnlyOldIsPresent(accessModifier)) {
			if (isOldPublic(accessModifier)) {
				return SemverStatus.MAJOR;
			} else {
				return SemverStatus.PATCH;
			}
		} else if (isOnlyNewPresent(accessModifier)) {
			if (isNewPublic(accessModifier)) {
				return SemverStatus.MINOR;
			} else {
				return SemverStatus.PATCH;
			}
		} else {
			throw new IllegalStateException();
		}
	}

	private static boolean isOldPublic(JApiModifier<AccessModifier> accessModifier) {
		return accessModifier.getOldModifier().get().equals(AccessModifier.PUBLIC);
	}

	private static boolean isNewPublic(JApiModifier<AccessModifier> accessModifier) {
		return accessModifier.getNewModifier().get().equals(AccessModifier.PUBLIC);
	}

	private static boolean isOnlyNewPresent(JApiModifier<AccessModifier> accessModifier) {
		return !isNewPresent(accessModifier) && isOldPresent(accessModifier);
	}

	private static boolean isOldPresent(JApiModifier<AccessModifier> accessModifier) {
		return accessModifier.getNewModifier().isPresent();
	}

	private static boolean isNewPresent(JApiModifier<AccessModifier> accessModifier) {
		return accessModifier.getOldModifier().isPresent();
	}

	private static boolean ifOnlyOldIsPresent(JApiModifier<AccessModifier> accessModifier) {
		return isNewPresent(accessModifier) && !isOldPresent(accessModifier);
	}

	private static boolean isOldAndNewPresent(JApiModifier<AccessModifier> accessModifier) {
		return isNewPresent(accessModifier) && isOldPresent(accessModifier);
	}

	private SemverStatus processSuperclassChanges(JApiClass jApiClass) {
		JApiSuperclass jApiSuperclass = jApiClass.getSuperclass();
		return detectChangeStatus(jApiSuperclass, jApiClass);
	}

	private ImmutableSet<SemverStatus> processInterfaceChanges(JApiClass jApiClass) {
		List<JApiImplementedInterface> interfaces = jApiClass.getInterfaces();
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		for (JApiImplementedInterface implementedInterface : interfaces) {
			builder.add(detectChangeStatus(implementedInterface, jApiClass));
		}
		return builder.build();
	}

	static enum SemverStatus {
		PATCH, MINOR, MAJOR;
	}

}
