package addon.roo.audittimestamp;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.classpath.PhysicalTypeDetails;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.PhysicalTypeMetadata;
import org.springframework.roo.classpath.PhysicalTypeMetadataProvider;
import org.springframework.roo.classpath.TypeLocationService;
import org.springframework.roo.classpath.details.MemberFindingUtils;
import org.springframework.roo.classpath.details.MutableClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadataBuilder;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.DependencyScope;
import org.springframework.roo.project.DependencyType;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.project.Repository;
import org.springframework.roo.support.util.Assert;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Element;

/**
 * Implementation of operations this add-on offers.
 *
 * @since 1.1
 */
@Component // Use these Apache Felix annotations to register your commands class in the Roo container
@Service
public class AudittimestampOperationsImpl implements AudittimestampOperations {
	
	/**
	 * MetadataService offers access to Roo's metadata model, use it to retrieve any available metadata by its MID
	 */
	@Reference private MetadataService metadataService;
	
	/**
	 * Use the PhysicalTypeMetadataProvider to access information about a physical type in the project
	 */
	@Reference private PhysicalTypeMetadataProvider physicalTypeMetadataProvider;
	
	/**
	 * Use ProjectOperations to install new dependencies, plugins, properties, etc into the project configuration
	 */
	@Reference private ProjectOperations projectOperations;

	/**
	 * Use TypeLocationService to find types which are annotated with a given annotation in the project
	 */
	@Reference private TypeLocationService typeLocationService;
	
	@Reference private FileManager fileManager;

	/** {@inheritDoc} */
	public boolean isCommandAvailable() {
		// Check if a project has been created
		return projectOperations.isProjectAvailable() && fileManager.exists(projectOperations.getPathResolver().getIdentifier(Path.SRC_MAIN_RESOURCES, "META-INF/persistence.xml"));
	}

	/** {@inheritDoc} */
	public void annotateType(JavaType javaType) {
		// Use Roo's Assert type for null checks
		Assert.notNull(javaType, "Java type required");

		// Retrieve metadata for the Java source type the annotation is being added to
		String id = physicalTypeMetadataProvider.findIdentifier(javaType);
		if (id == null) {
			throw new IllegalArgumentException("Cannot locate source for '" + javaType.getFullyQualifiedTypeName() + "'");
		}

		// Obtain the physical type and itd mutable details
		PhysicalTypeMetadata physicalTypeMetadata = (PhysicalTypeMetadata) metadataService.get(id);
		Assert.notNull(physicalTypeMetadata, "Java source code unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		
		// Obtain physical type details for the target type
		PhysicalTypeDetails physicalTypeDetails = physicalTypeMetadata.getMemberHoldingTypeDetails();
		Assert.notNull(physicalTypeDetails, "Java source code details unavailable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		
		// Test if the type is an MutableClassOrInterfaceTypeDetails instance so the annotation can be added
		Assert.isInstanceOf(MutableClassOrInterfaceTypeDetails.class, physicalTypeDetails, "Java source code is immutable for type " + PhysicalTypeIdentifier.getFriendlyName(id));
		MutableClassOrInterfaceTypeDetails mutableTypeDetails = (MutableClassOrInterfaceTypeDetails) physicalTypeDetails;

		// Test if the annotation already exists on the target type
		if (MemberFindingUtils.getAnnotationOfType(mutableTypeDetails.getAnnotations(), new JavaType(RooAuditTimeStamp.class.getName())) == null) {
			
			// Create JavaType instance for the add-ons trigger annotation
			JavaType rooRooAudittimestamp = new JavaType(RooAuditTimeStamp.class.getName());

			// Create Annotation metadata
			AnnotationMetadataBuilder annotationBuilder = new AnnotationMetadataBuilder(rooRooAudittimestamp);
			
			// Add annotation to target type
			mutableTypeDetails.addTypeAnnotation(annotationBuilder.build());
		}
	}

	/** {@inheritDoc} */
	public void annotateAll() {
		// Use the TypeLocationService to scan project for all types with a specific annotation
		for (JavaType type: typeLocationService.findTypesWithAnnotation(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean"))) {
			annotateType(type);
		}
	}
	
	/** {@inheritDoc} */
	public void setup() {
		// Install the add-on Google code repository needed to get the annotation 
		projectOperations.addRepository(new Repository("Audittimestamp Roo add-on repository", "Audittimestamp Roo add-on repository", "https://spring-roo-addon-audit-timestamp.googlecode.com/svn/repo"));
		
		List<Dependency> dependencies = new ArrayList<Dependency>();
		
		// Install the dependency on the add-on jar (
		dependencies.add(new Dependency("addon.roo.audittimestamp", "addon.roo.audittimestamp", "0.1.1.BUILD", DependencyType.JAR, DependencyScope.PROVIDED));
		
		// Install dependencies defined in external XML file
		for (Element dependencyElement : XmlUtils.findElements("/configuration/batch/dependencies/dependency", XmlUtils.getConfiguration(getClass()))) {
			dependencies.add(new Dependency(dependencyElement));
		}

		// Add all new dependencies to pom.xml
		projectOperations.addDependencies(dependencies);
	}
}