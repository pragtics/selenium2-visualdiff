package org.kreyssel.selenium.visualdiff.mojo;

import com.google.common.collect.ImmutableListMultimap;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.metadata.ArtifactMetadataRetrievalException;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.*;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.doxia.site.decoration.DecorationModel;
import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.doxia.siterenderer.RenderingContext;
import org.apache.maven.doxia.siterenderer.SiteRenderingContext;
import org.apache.maven.doxia.siterenderer.sink.SiteRendererSink;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.mojo.versions.api.ArtifactVersions;
import org.codehaus.mojo.versions.ordering.VersionComparators;
import org.kreyssel.selenium.visualdiff.core.ScreenshotStore;
import org.kreyssel.selenium.visualdiff.core.diff.VisualDiff;
import org.kreyssel.selenium.visualdiff.core.diff.VisualDiffMeta;
import org.kreyssel.selenium.visualdiff.core.diff.VisualDiffMetaGrouper;
import org.kreyssel.selenium.visualdiff.mojo.report.VisualDiffReportRenderer;
import org.kreyssel.selenium.visualdiff.mojo.report.VisualDiffReportUtil;
import org.kreyssel.selenium.visualdiff.mojo.report.VisualDiffTestReportRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * ReportMojo generates a report of visual diffs between two selenium2
 * functional tests runs.
 * 
 * @goal visualdiff-report
 * @phase site
 */
public class ReportMojo extends AbstractMavenReport {

	/**
	 * Directory where reports will go.
	 * 
	 * @parameter property="project.reporting.outputDirectory"
	 * @required
	 * @readonly
	 */
	private String outputDirectory;

	/**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;

	/**
	 * Used to look up Artifacts in the remote repository.
	 * @component
	 * @required
	 * @readonly
	 */
	protected RepositorySystem repositorySystem;

	/**
	 * @component
	 * @since 1.0-alpha-3
	 */
	private ArtifactResolver artifactResolver;

	/**
	 * The artifact metadata source to use.
	 * 
	 * @component
	 * @required
	 * @readonly
	 */
	private ArtifactMetadataSource artifactMetadataSource;

	/**
	 * @component
	 * @required
	 * @readonly
	 */
	private Renderer siteRenderer;

	public String getDescription(final Locale arg0) {
		return "desc";
	}

	public String getName(final Locale arg0) {
		return "Selenium2 Visuall Diff";
	}

	public String getOutputName() {
		return "visualdiff";
	}

	@Override
	protected String getOutputDirectory() {
		return outputDirectory;
	}

	@Override
	protected MavenProject getProject() {
		return project;
	}

	@Override
	protected Renderer getSiteRenderer() {
		return siteRenderer;
	}

	@Override
	protected void executeReport(final Locale arg0) throws MavenReportException {

		Artifact artifact = project.getArtifact();

		Artifact currentArtifact;
		try {
			currentArtifact = resolveScreenshotArtifact(artifact, project.getVersion());
		} catch (Exception ex) {
			throw new MavenReportException(
					"Error on resolve screenshot artifact for current project!", ex);
		}

		if (currentArtifact == null) {
			throw new MavenReportException(
					"Could not found screenshot archive! Did you ensure that you run the package goal before?");
		}

		Artifact previousArtifact;
		try {
			previousArtifact = getScreenshotsFromLatestRelease(artifact);
		} catch (Exception ex) {
			throw new MavenReportException(
					"Error on resolve screenshot artifact for latest project!", ex);
		}

		if (previousArtifact == null || previousArtifact.getFile() == null) {
			getLog().warn(
					"Could not found a previous release version of artifact '"
							+ project.getArtifact() + "'!");
			return;
		}

		ScreenshotStore currentScreenshotsStore = new ScreenshotStore(currentArtifact.getFile());
		ScreenshotStore previousScreenshotsStore = new ScreenshotStore(
				previousArtifact.getFile());
		VisualDiff vd = new VisualDiff(new File(outputDirectory, "images/visualdiff"));

		// render overview
		getLog().info("Render visual diff overview ...");

		List<VisualDiffMeta> diffs;
		try {
			diffs = vd.diff(currentScreenshotsStore, previousScreenshotsStore);
		} catch (IOException ex) {
			throw new MavenReportException("Error on diff screenshots!", ex);
		}

		new VisualDiffReportRenderer(getSink(), currentArtifact, previousArtifact, diffs).render();


		SiteRenderingContext siteContext = new SiteRenderingContext();
		siteContext.setDecoration( new DecorationModel() );
		siteContext.setTemplateName( "org/apache/maven/doxia/siterenderer/resources/default-site.vm" );
		siteContext.setTemplateProperties(getTemplateProperties());

		// render report per testclass
		ImmutableListMultimap<String, VisualDiffMeta> groupedPerTest = VisualDiffMetaGrouper
				.byTestClass(diffs);
		for (String testClass : groupedPerTest.keySet()) {
			getLog().info("Render visual diff result for test '" + testClass + "' ...");

			String filename = VisualDiffReportUtil.asFilename(testClass, ".html");

			RenderingContext context = new RenderingContext(new File(outputDirectory), filename);
			SiteRendererSink sinkForTestClass = new SiteRendererSink(context);
			new VisualDiffTestReportRenderer(sinkForTestClass, testClass, groupedPerTest.get(testClass)).render();

			try {
				OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(new File(outputDirectory, filename)), getOutputEncoding());
				getSiteRenderer().generateDocument(outputStreamWriter, sinkForTestClass, siteContext);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	protected Artifact getScreenshotsFromLatestRelease(final Artifact artifact)
			throws ArtifactMetadataRetrievalException, ArtifactResolutionException,
			ArtifactNotFoundException, org.eclipse.aether.resolution.ArtifactResolutionException, InvalidRepositoryException {

		// resolve previous version of screenshots.zip via maven metadata
		ArtifactVersions artifactVersions = new ArtifactVersions(artifact,
				artifactMetadataSource.retrieveAvailableVersions(artifact, repositorySystem.createDefaultLocalRepository(), Collections.singletonList(repositorySystem.createDefaultRemoteRepository())),
				VersionComparators.getVersionComparator("maven"));

		ArtifactVersion newestArtifactVersion = artifactVersions.getNewestVersion(null, artifact.getSelectedVersion(), true);

		if (newestArtifactVersion == null) {
			return null;
		}

		return resolveScreenshotArtifact(artifact, newestArtifactVersion.toString());
	}

	protected Artifact resolveScreenshotArtifact(final Artifact artifact, final String version)
			throws ArtifactNotFoundException, ArtifactResolutionException, InvalidRepositoryException {

		// resolve screenshots.zip artifact
		Artifact resolveArtifact = repositorySystem.createArtifactWithClassifier(
				artifact.getGroupId(), artifact.getArtifactId(), version, "zip", "screenshots");


		ArtifactResolutionResult resolve = repositorySystem.resolve(new ArtifactResolutionRequest().setArtifact(resolveArtifact));
//		artifactResolver.resolve(resolveArtifact, repositorySystem.createDefaultLocalRepository(), localRepository);

		return resolveArtifact;
	}

	private Map<String, Object> getTemplateProperties()
	{
		Map<String, Object> templateProperties = new HashMap<String, Object>();
		templateProperties.put( "project", getProject() );
		templateProperties.put( "inputEncoding", getInputEncoding() );
		templateProperties.put( "outputEncoding", getOutputEncoding() );
		// Put any of the properties in directly into the Velocity context
		for ( Map.Entry<Object, Object> entry : getProject().getProperties().entrySet() )
		{
			templateProperties.put( (String) entry.getKey(), entry.getValue() );
		}
		return templateProperties;
	}
}
