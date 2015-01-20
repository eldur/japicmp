package japicmp.output.semver;

import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import japicmp.model.JApiAnnotation;
import japicmp.model.JApiAnnotationElement;
import japicmp.model.JApiBinaryCompatibility;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiClass;
import japicmp.model.JApiConstructor;
import japicmp.model.JApiField;
import japicmp.model.JApiHasAnnotations;
import japicmp.model.JApiHasChangeStatus;
import japicmp.model.JApiImplementedInterface;
import japicmp.model.JApiMethod;
import japicmp.model.JApiSuperclass;
import japicmp.output.Output;

public class SemverOut implements Output {

	@Override
	public Optional<String> generate(ImmutableList<JApiClass> jApiClasses) {
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		for (JApiClass jApiClass : jApiClasses) {
			builder.addAll(processClass(jApiClass));
			builder.addAll(processConstructors(jApiClass));
			builder.addAll(processMethods(jApiClass));
			builder.addAll(processAnnotations(jApiClass));
		}
		ImmutableSet<SemverStatus> build = builder.build();
		if (build.contains(SemverStatus.CHANGED_BINARY_INCOMPATIBLE)) {
			return Optional.of("1.0.0");
		} else if (build.contains(SemverStatus.CHANGED_BINARY_COMPATIBLE)) {
			return Optional.of("0.1.0");
		} else if (build.isEmpty() || build.contains(SemverStatus.UNCHANGED)) {
			return Optional.of("0.0.1");
		} else {
			return Optional.of("N/A");
		}
	}

	private ImmutableSet<SemverStatus> processAnnotations(JApiHasAnnotations jApiClass) {
		List<JApiAnnotation> annotations = jApiClass.getAnnotations();
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		for (JApiAnnotation jApiAnnotation : annotations) {
			builder.add(signs(jApiAnnotation));
			List<JApiAnnotationElement> elements = jApiAnnotation.getElements();
			for (JApiAnnotationElement jApiAnnotationElement : elements) {
				builder.add(signs(jApiAnnotationElement));
			}
			if (Deprecated.class.getCanonicalName().equals(jApiAnnotation.getFullyQualifiedName())) {
				if (jApiAnnotation.getChangeStatus().equals(JApiChangeStatus.NEW)) {
					builder.add(SemverStatus.CHANGED_BINARY_COMPATIBLE);
				}
			}
		}
		return builder.build();
	}

	private ImmutableSet<SemverStatus> processConstructors(JApiClass jApiClass) {
		List<JApiConstructor> constructors = jApiClass.getConstructors();
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		for (JApiConstructor jApiConstructor : constructors) {
			builder.add(signs(jApiConstructor));
			builder.addAll(processAnnotations(jApiConstructor));
		}
		return builder.build();
	}

	private ImmutableSet<SemverStatus> processMethods(JApiClass jApiClass) {
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		List<JApiMethod> methods = jApiClass.getMethods();
		for (JApiMethod jApiMethod : methods) {
			builder.add(signs(jApiMethod));
			builder.addAll(processAnnotations(jApiMethod));
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

	/**
	 * Visible for testing
	 */
	static SemverStatus signs(JApiHasChangeStatus hasChangeStatus) {
		JApiChangeStatus changeStatus = hasChangeStatus.getChangeStatus();
		switch (changeStatus) {
		case UNCHANGED:
			return SemverStatus.UNCHANGED;
		case NEW:
		case REMOVED:
		case MODIFIED:
			if (hasChangeStatus instanceof JApiBinaryCompatibility) {
				JApiBinaryCompatibility binaryCompatibility = (JApiBinaryCompatibility) hasChangeStatus;
				if (binaryCompatibility.isBinaryCompatible()) {
					return SemverStatus.CHANGED_BINARY_COMPATIBLE;
				} else {
					return SemverStatus.CHANGED_BINARY_INCOMPATIBLE;
				}
			} else {
				throw new IllegalStateException(
						"Element '" + hasChangeStatus.getClass().getCanonicalName() + " does not implement '" +
								JApiBinaryCompatibility.class.getCanonicalName() + "'.");
			}
		default:
			throw new IllegalStateException("The following JApiChangeStatus is not supported: " +
					(changeStatus == null ? "null" : changeStatus.name()));
		}
	}

	private ImmutableSet<SemverStatus> processFieldChanges(JApiClass jApiClass) {
		List<JApiField> fields = jApiClass.getFields();
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		for (JApiField field : fields) {
			builder.add(signs(field));
			builder.addAll(processAnnotations(field));
		}
		return builder.build();
	}

	private SemverStatus processSuperclassChanges(JApiClass jApiClass) {
		JApiSuperclass jApiSuperclass = jApiClass.getSuperclass();
		return signs(jApiSuperclass);
	}

	private ImmutableSet<SemverStatus> processInterfaceChanges(JApiClass jApiClass) {
		List<JApiImplementedInterface> interfaces = jApiClass.getInterfaces();
		ImmutableSet.Builder<SemverStatus> builder = ImmutableSet.builder();
		for (JApiImplementedInterface implementedInterface : interfaces) {
			builder.add(signs(implementedInterface));
		}
		return builder.build();
	}

	/**
	 * Visible for testing
	 */
	static enum SemverStatus {
		UNCHANGED, CHANGED_BINARY_COMPATIBLE, CHANGED_BINARY_INCOMPATIBLE;
	}
}
