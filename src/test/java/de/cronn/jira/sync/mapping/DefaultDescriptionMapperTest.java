package de.cronn.jira.sync.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import de.cronn.jira.sync.config.JiraSyncConfig;
import de.cronn.jira.sync.domain.JiraIssue;
import de.cronn.jira.sync.service.JiraService;

@RunWith(MockitoJUnitRunner.class)
public class DefaultDescriptionMapperTest {

	@Mock
	private JiraService jiraSource;

	@Spy
	private UsernameReplacer usernameReplacer = new DefaultUsernameReplacer();

	@Spy
	private TicketReferenceReplacer ticketReferenceReplacer = new DefaultTicketReferenceReplacer();

	@Spy
	private JiraSyncConfig jiraSyncConfig = new JiraSyncConfig();

	@InjectMocks
	private DefaultDescriptionMapper descriptionMapper;

	@Before
	public void tearDown() {
		verifyNoMoreInteractions(jiraSource);
	}

	@Test
	public void testMapSourceDescription_HappyCase() throws Exception {
		String description = descriptionMapper.mapSourceDescription("some description", jiraSource);
		assertThat(description).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nsome description\n{panel}\n\n");
	}

	@Test
	public void testMapSourceDescription_DifferentColors() throws Exception {
		jiraSyncConfig.getDescriptionMapping().setPanelBackgroundColor("#ffffff");
		jiraSyncConfig.getDescriptionMapping().setPanelTitleBackgroundColor("#eeeeee");
		String description = descriptionMapper.mapSourceDescription("some description", jiraSource);
		assertThat(description).isEqualTo("{panel:title=Original description|titleBGColor=#eeeeee|bgColor=#ffffff}\nsome description\n{panel}\n\n");
	}

	@Test
	public void testMapSourceDescription_Newline() throws Exception {
		String description = descriptionMapper.mapSourceDescription("some\ndescription\r\nnewline\n", jiraSource);
		assertThat(description).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nsome\ndescription\nnewline\n{panel}\n\n");
	}

	@Test
	public void testMapSourceDescription_PanelTagInSourceDescription() throws Exception {
		String description = descriptionMapper.mapSourceDescription("some description with {panel:title=foo}bla bar\ntest{panel}", jiraSource);
		assertThat(description).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nsome description with \\{panel:title=foo\\}bla bar\ntest\\{panel\\}\n{panel}\n\n");
	}

	@Test
	public void testMapSourceDescription_Null() throws Exception {
		String description = descriptionMapper.mapSourceDescription((String) null, jiraSource);
		assertThat(description).isEqualTo("");
	}

	@Test
	public void testMapSourceDescription_NullFields() throws Exception {
		JiraIssue jiraIssue = new JiraIssue();

		assertThatExceptionOfType(IllegalArgumentException.class)
			.isThrownBy(() -> descriptionMapper.mapSourceDescription(jiraIssue, jiraSource))
			.withMessage("fields must not be null");
	}

	@Test
	public void testMapTargetDescription_EmptySource_EmptyTarget() throws Exception {
		String description = descriptionMapper.mapTargetDescription(null, (String) null, jiraSource);
		assertThat(description).isNull();
	}

	@Test
	public void testMapTargetDescription_OldTargetFormat() throws Exception {
		String description = descriptionMapper.mapTargetDescription("source\ndescription", "source\ndescription", jiraSource);
		assertThat(description).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nsource\ndescription\n{panel}");
	}

	@Test
	public void testMapTargetDescription_NonEmptySource_EmptyTarget() throws Exception {
		String description = descriptionMapper.mapTargetDescription("some description", null, jiraSource);
		assertThat(description).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nsome description\n{panel}");
	}

	@Test
	public void testMapTargetDescription_EmptySource_NonEmptyTarget() throws Exception {
		String description = descriptionMapper.mapTargetDescription(null, "some description", jiraSource);
		assertThat(description).isEqualTo("some description");
	}

	@Test
	public void testMapTargetDescription_NonEmptySource_NonEmptyTarget() throws Exception {
		String description = descriptionMapper.mapTargetDescription("source description", "target description", jiraSource);
		assertThat(description).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nsource description\n{panel}\n\ntarget description");
	}

	@Test
	public void testMapTargetDescription_ChangedSource_ExistingTarget() throws Exception {
		String sourceDescription = "changed source description";
		String targetDescription = "{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nsource description\n{panel}\ntarget description";
		String description = descriptionMapper.mapTargetDescription(sourceDescription, targetDescription, jiraSource);
		assertThat(description).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nchanged source description\n{panel}\n\ntarget description");
	}

	@Test
	public void testMapTargetDescription_ExistingSource_ExistingTarget() throws Exception {
		String sourceDescription = "source description $_@-äüö";
		String targetDescription = descriptionMapper.mapSourceDescription(sourceDescription, jiraSource).trim();
		String description = descriptionMapper.mapTargetDescription(sourceDescription, targetDescription, jiraSource);
		assertThat(description).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nsource description $_@-äüö\n{panel}");
	}

	@Test
	public void testMapTargetDescription_ChangedSource_ExistingTarget_AlsoTextAboveOriginalDescription() throws Exception {
		String sourceDescription = "changed source description";
		String targetDescription = "Some text above\n{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nsource description\n{panel}\ntarget description";
		String description = descriptionMapper.mapTargetDescription(sourceDescription, targetDescription, jiraSource);
		assertThat(description).isEqualTo("Some text above\n{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nchanged source description\n{panel}\n\ntarget description");
	}

	@Test
	public void testMapTargetDescription_ChangedSource_ExistingTarget_BrokenWhiteSpace() throws Exception {
		String sourceDescription = "changed source description";
		String targetDescription = "{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nsource description\n{panel}     target description";
		String description = descriptionMapper.mapTargetDescription(sourceDescription, targetDescription, jiraSource);
		assertThat(description).isEqualTo("{panel:title=Original description|titleBGColor=#dddddd|bgColor=#eeeeee}\nchanged source description\n{panel}\n\ntarget description");
	}

}