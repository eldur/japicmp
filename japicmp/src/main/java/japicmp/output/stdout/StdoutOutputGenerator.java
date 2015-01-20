package japicmp.output.stdout;

import java.io.File;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import japicmp.config.ImmutableOptions;
import japicmp.config.Options;
import japicmp.model.*;
import japicmp.model.JApiAnnotationElementValue.Type;
import japicmp.output.OutputFilter;
import javassist.bytecode.annotation.MemberValue;

public class StdoutOutputGenerator {
    private final ImmutableOptions options;

    public StdoutOutputGenerator(Options options) {
        this(ImmutableOptions.toImmutable(options));
    }

    public StdoutOutputGenerator(ImmutableOptions options) {
        this.options = options;
    }

    public String generate(File oldArchive, File newArchive, List<JApiClass> jApiClasses) {
        OutputFilter outputFilter = new OutputFilter(options);
        List<JApiClass> classListForModification = Lists.newArrayList(jApiClasses);
        outputFilter.filter(classListForModification);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Comparing %s with %s:%n", oldArchive.getAbsolutePath(), newArchive.getAbsolutePath()));
        for (JApiClass jApiClass : classListForModification) {
            processClass(sb, jApiClass);
            processConstructors(sb, jApiClass);
            processMethods(sb, jApiClass);
            processAnnotations(sb, jApiClass, 1);
        }
        return sb.toString();
    }

    private void processAnnotations(StringBuilder sb, JApiHasAnnotations jApiClass, int numberofTabs) {
        List<JApiAnnotation> annotations = jApiClass.getAnnotations();
        for (JApiAnnotation jApiAnnotation : annotations) {
            appendAnnotation(sb, signs(jApiAnnotation), jApiAnnotation, numberofTabs);
            List<JApiAnnotationElement> elements = jApiAnnotation.getElements();
            for (JApiAnnotationElement jApiAnnotationElement : elements) {
                appendAnnotationElement(sb, signs(jApiAnnotationElement), jApiAnnotationElement, numberofTabs + 1);
            }
        }
    }

    private void processConstructors(StringBuilder sb, JApiClass jApiClass) {
        List<JApiConstructor> constructors = jApiClass.getConstructors();
        for (JApiConstructor jApiConstructor : constructors) {
            appendMethod(sb, signs(jApiConstructor), jApiConstructor, "CONSTRUCTOR:");
            processAnnotations(sb, jApiConstructor, 2);
        }
    }

    private void processMethods(StringBuilder sb, JApiClass jApiClass) {
        List<JApiMethod> methods = jApiClass.getMethods();
        for (JApiMethod jApiMethod : methods) {
            appendMethod(sb, signs(jApiMethod), jApiMethod, "METHOD:");
            processAnnotations(sb, jApiMethod, 2);
        }
    }

    private void processClass(StringBuilder sb, JApiClass jApiClass) {
        appendClass(sb, signs(jApiClass), jApiClass);
    }

    private String signs(JApiHasChangeStatus hasChangeStatus) {
        JApiChangeStatus changeStatus = hasChangeStatus.getChangeStatus();
        String retVal = "???";
        switch (changeStatus) {
            case UNCHANGED:
                retVal = "===";
                break;
            case NEW:
                retVal = "+++";
                break;
            case REMOVED:
                retVal = "---";
                break;
            case MODIFIED:
                retVal = "***";
                break;
        }
        boolean binaryCompatible = true;
        if (hasChangeStatus instanceof JApiBinaryCompatibility) {
            JApiBinaryCompatibility binaryCompatibility = (JApiBinaryCompatibility) hasChangeStatus;
            binaryCompatible = binaryCompatibility.isBinaryCompatible();
        }
        if (binaryCompatible) {
            retVal += " ";
        } else {
            retVal += "!";
        }
        return retVal;
    }

    private void appendMethod(StringBuilder sb, String signs, JApiBehavior jApiBehavior, String classMemberType) {
        sb.append("\t" + signs + " " + jApiBehavior.getChangeStatus() + " " + classMemberType + " " + accessModifierAsString(jApiBehavior) + abstractModifierAsString(jApiBehavior)
                + staticModifierAsString(jApiBehavior) + finalModifierAsString(jApiBehavior) + returnType(jApiBehavior) + jApiBehavior.getName() + "(");
        int paramCount = 0;
        for (JApiParameter jApiParameter : jApiBehavior.getParameters()) {
            if (paramCount > 0) {
                sb.append(", ");
            }
            sb.append(jApiParameter.getType());
            paramCount++;
        }
        sb.append(")\n");
    }
    
    private String returnType(JApiBehavior jApiBehavior) {
    	if(jApiBehavior instanceof JApiMethod) {
    		JApiMethod method = (JApiMethod)jApiBehavior;
    		return method.getReturnType() + " ";
    	}
    	return "";
    }

    private void appendAnnotation(StringBuilder sb, String signs, JApiAnnotation jApiAnnotation, int numberOfTabs) {
        sb.append(tabs(numberOfTabs) + signs + " " + jApiAnnotation.getChangeStatus() + " ANNOTATION: " + jApiAnnotation.getFullyQualifiedName() + "\n");
    }

    private void appendAnnotationElement(StringBuilder sb, String signs, JApiAnnotationElement jApiAnnotationElement, int numberOfTabs) {
        sb.append(tabs(numberOfTabs) + signs + " " + jApiAnnotationElement.getChangeStatus() + " ELEMENT: " + jApiAnnotationElement.getName() + "=");
        Optional<MemberValue> oldValue = jApiAnnotationElement.getOldValue();
        Optional<MemberValue> newValue = jApiAnnotationElement.getNewValue();
        if (oldValue.isPresent() && newValue.isPresent()) {
            if (jApiAnnotationElement.getChangeStatus() == JApiChangeStatus.UNCHANGED) {
                sb.append(elementValueList2String(jApiAnnotationElement.getNewElementValues()));
            } else if (jApiAnnotationElement.getChangeStatus() == JApiChangeStatus.REMOVED) {
                sb.append(elementValueList2String(jApiAnnotationElement.getOldElementValues()) + " (-)");
            } else if (jApiAnnotationElement.getChangeStatus() == JApiChangeStatus.NEW) {
                sb.append(elementValueList2String(jApiAnnotationElement.getNewElementValues()) + " (+)");
            } else if (jApiAnnotationElement.getChangeStatus() == JApiChangeStatus.MODIFIED) {
                sb.append(elementValueList2String(jApiAnnotationElement.getNewElementValues()) + " (<- " + elementValueList2String(jApiAnnotationElement.getOldElementValues()) + ")");
            }
        } else if (!oldValue.isPresent() && newValue.isPresent()) {
            sb.append(elementValueList2String(jApiAnnotationElement.getNewElementValues()) + " (+)");
        } else if (oldValue.isPresent() && !newValue.isPresent()) {
            sb.append(elementValueList2String(jApiAnnotationElement.getOldElementValues()) + " (-)");
        } else {
            sb.append(" n.a.");
        }
        sb.append("\n");
    }
    
	private String elementValueList2String(List<JApiAnnotationElementValue> values) {
		StringBuilder sb = new StringBuilder();
		for (JApiAnnotationElementValue value : values) {
			if (sb.length() > 0) {
				sb.append(",");
			}
			if (value.getName().isPresent()) {
				sb.append(value.getName().get() + "=");
			}
			if (value.getType() != Type.Array && value.getType() != Type.Annotation) {
				if (value.getType() == Type.Enum) {
					sb.append(value.getFullyQualifiedName() + "." + value.getValueString());
				} else {
					sb.append(value.getValueString());
				}
			} else {
				if (value.getType() == Type.Array) {
					sb.append("{" + elementValueList2String(value.getValues()) + "}");
				} else if (value.getType() == Type.Annotation) {
					sb.append("@" + value.getFullyQualifiedName() + "(" + elementValueList2String(value.getValues()) + ")");
				}
			}
		}
		return sb.toString();
	}

	private String tabs(int numberOfTabs) {
        if (numberOfTabs <= 0) {
            return "";
        } else if (numberOfTabs == 1) {
            return "\t";
        } else if (numberOfTabs == 2) {
            return "\t\t";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numberOfTabs; i++) {
                sb.append("\t");
            }
            return sb.toString();
        }
    }

    private void appendClass(StringBuilder sb, String signs, JApiClass jApiClass) {
        sb.append(signs + " " + jApiClass.getChangeStatus() + " " + jApiClass.getType() + ": " + accessModifierAsString(jApiClass) + abstractModifierAsString(jApiClass)
                + staticModifierAsString(jApiClass) + finalModifierAsString(jApiClass) + jApiClass.getFullyQualifiedName() + "\n");
        processInterfaceChanges(sb, jApiClass);
        processSuperclassChanges(sb, jApiClass);
        processFieldChanges(sb, jApiClass);
    }

    private void processFieldChanges(StringBuilder sb, JApiClass jApiClass) {
        List<JApiField> fields = jApiClass.getFields();
        for (JApiField field : fields) {
            sb.append(tabs(1) + signs(field) + " " + field.getChangeStatus() + " FIELD: " + accessModifierAsString(field) + staticModifierAsString(field)
                    + finalModifierAsString(field) + fieldTypeChangeAsString(field) + field.getName() + "\n");
            processAnnotations(sb, field, 2);
        }
    }

    private String abstractModifierAsString(JApiHasAbstractModifier hasAbstractModifier) {
        JApiModifier<AbstractModifier> modifier = hasAbstractModifier.getAbstractModifier();
        return modifierAsString(modifier, AbstractModifier.NON_ABSTRACT);
    }

    private String finalModifierAsString(JApiHasFinalModifier hasFinalModifier) {
        JApiModifier<FinalModifier> modifier = hasFinalModifier.getFinalModifier();
        return modifierAsString(modifier, FinalModifier.NON_FINAL);
    }

    private String staticModifierAsString(JApiHasStaticModifier hasStaticModifier) {
        JApiModifier<StaticModifier> modifier = hasStaticModifier.getStaticModifier();
        return modifierAsString(modifier, StaticModifier.NON_STATIC);
    }

    private String accessModifierAsString(JApiHasAccessModifier modifier) {
        JApiModifier<AccessModifier> accessModifier = modifier.getAccessModifier();
        return modifierAsString(accessModifier, AccessModifier.PACKAGE_PROTECTED);
    }

    private <T> String modifierAsString(JApiModifier<T> accessModifier, T notPrintValue) {
        if (accessModifier.getOldModifier().isPresent() && accessModifier.getNewModifier().isPresent()) {
            if (accessModifier.getChangeStatus() == JApiChangeStatus.MODIFIED) {
                return accessModifier.getNewModifier().get() + " (<- " + accessModifier.getOldModifier().get() + ") ";
            } else if (accessModifier.getChangeStatus() == JApiChangeStatus.NEW) {
                if (accessModifier.getNewModifier().get() != notPrintValue) {
                    return accessModifier.getNewModifier().get() + "(+) ";
                }
            } else if (accessModifier.getChangeStatus() == JApiChangeStatus.REMOVED) {
                if (accessModifier.getOldModifier().get() != notPrintValue) {
                    return accessModifier.getOldModifier().get() + "(-) ";
                }
            } else {
                if (accessModifier.getNewModifier().get() != notPrintValue) {
                    return accessModifier.getNewModifier().get() + " ";
                }
            }
        } else if (accessModifier.getOldModifier().isPresent() && !accessModifier.getNewModifier().isPresent()) {
            if (accessModifier.getOldModifier().get() != notPrintValue) {
                return accessModifier.getOldModifier().get() + "(-) ";
            }
        } else if (!accessModifier.getOldModifier().isPresent() && accessModifier.getNewModifier().isPresent()) {
            if (accessModifier.getNewModifier().get() != notPrintValue) {
                return accessModifier.getNewModifier().get() + "(+) ";
            }
        }
        return "";
    }

    private String fieldTypeChangeAsString(JApiField field) {
        JApiType type = field.getType();
        if (type.getOldTypeOptional().isPresent() && type.getNewTypeOptional().isPresent()) {
            if (type.getChangeStatus() == JApiChangeStatus.MODIFIED) {
                return type.getNewTypeOptional().get() + " (<- " + type.getOldTypeOptional().get() + ") ";
            } else if (type.getChangeStatus() == JApiChangeStatus.NEW) {
                return type.getNewTypeOptional().get() + "(+) ";
            } else if (type.getChangeStatus() == JApiChangeStatus.REMOVED) {
                return type.getOldTypeOptional().get() + "(-) ";
            } else {
                return type.getNewTypeOptional().get() + " ";
            }
        } else if (type.getOldTypeOptional().isPresent() && !type.getNewTypeOptional().isPresent()) {
            return type.getOldTypeOptional().get() + " ";
        } else if (!type.getOldTypeOptional().isPresent() && type.getNewTypeOptional().isPresent()) {
            return type.getNewTypeOptional().get() + " ";
        }
        return "n.a.";
    }

    private void processSuperclassChanges(StringBuilder sb, JApiClass jApiClass) {
        JApiSuperclass jApiSuperclass = jApiClass.getSuperclass();
        if (options.isOutputOnlyModifications() && jApiSuperclass.getChangeStatus() != JApiChangeStatus.UNCHANGED) {
            sb.append(tabs(1) + signs(jApiSuperclass) + " " + jApiSuperclass.getChangeStatus() + " SUPERCLASS: " + superclassChangeAsString(jApiSuperclass) + "\n");
        }
    }

    private String superclassChangeAsString(JApiSuperclass jApiSuperclass) {
        if (jApiSuperclass.getOldSuperclass().isPresent() && jApiSuperclass.getNewSuperclass().isPresent()) {
            return jApiSuperclass.getNewSuperclass().get() + " (<- " + jApiSuperclass.getOldSuperclass().get() + ")";
        } else if (jApiSuperclass.getOldSuperclass().isPresent() && !jApiSuperclass.getNewSuperclass().isPresent()) {
            return jApiSuperclass.getOldSuperclass().get();
        } else if (!jApiSuperclass.getOldSuperclass().isPresent() && jApiSuperclass.getNewSuperclass().isPresent()) {
            return jApiSuperclass.getNewSuperclass().get();
        }
        return "n.a.";
    }

    private void processInterfaceChanges(StringBuilder sb, JApiClass jApiClass) {
        List<JApiImplementedInterface> interfaces = jApiClass.getInterfaces();
        for (JApiImplementedInterface implementedInterface : interfaces) {
            sb.append(tabs(1) + signs(implementedInterface) + " " + implementedInterface.getChangeStatus() + " INTERFACE: " + implementedInterface.getFullyQualifiedName() + "\n");
        }
    }
}
