package de.cronn.jira.sync.mapping;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.cronn.jira.sync.domain.JiraIssue;

public class DefaultDescriptionMapperTest {

	private DefaultDescriptionMapper descriptionMapper;

	@Before
	public void setUp() {
		descriptionMapper = new DefaultDescriptionMapper();
	}

	@Test
	public void testMapSourceDescription_HappyCase() throws Exception {
		String description = descriptionMapper.mapSourceDescription("some description");
		assertThat(description, is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nsome description\n{panel}\n\n"));
	}

	@Test
	public void testMapSourceDescription_Newline() throws Exception {
		String description = descriptionMapper.mapSourceDescription("some\ndescription\r\nnewline\n");
		assertThat(description, is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nsome\ndescription\nnewline\n{panel}\n\n"));
	}

	@Test
	public void testMapSourceDescription_PanelTagInSourceDescription() throws Exception {
		String description = descriptionMapper.mapSourceDescription("some description with {panel:title=foo}bla bar\ntest{panel}");
		assertThat(description, is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nsome description with \\{panel:title=foo\\}bla bar\ntest\\{panel\\}\n{panel}\n\n"));
	}

	@Test
	public void testMapSourceDescription_Null() throws Exception {
		String description = descriptionMapper.mapSourceDescription((String) null);
		assertThat(description, is(""));
	}

	@Test
	public void testMapSourceDescription_NullFields() throws Exception {
		JiraIssue jiraIssue = new JiraIssue();

		try {
			descriptionMapper.mapSourceDescription(jiraIssue);
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			assertThat(e.getMessage(), is("fields must not be null"));
		}
	}

	@Test
	public void testMapTargetDescription_EmptySource_EmptyTarget() throws Exception {
		String description = descriptionMapper.mapTargetDescription(null, (String) null);
		assertNull(description);
	}

	@Test
	public void testMapTargetDescription_OldTargetFormat() throws Exception {
		String description = descriptionMapper.mapTargetDescription("source\ndescription", "source\ndescription");
		assertThat(description, is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nsource\ndescription\n{panel}"));
	}

	@Test
	public void testMapTargetDescription_NonEmptySource_EmptyTarget() throws Exception {
		String description = descriptionMapper.mapTargetDescription("some description", null);
		assertThat(description, is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nsome description\n{panel}"));
	}

	@Test
	public void testMapTargetDescription_EmptySource_NonEmptyTarget() throws Exception {
		String description = descriptionMapper.mapTargetDescription(null, "some description");
		assertThat(description, is("some description"));
	}

	@Test
	public void testMapTargetDescription_NonEmptySource_NonEmptyTarget() throws Exception {
		String description = descriptionMapper.mapTargetDescription("source description", "target description");
		assertThat(description, is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nsource description\n{panel}\n\ntarget description"));
	}

	@Test
	public void testMapTargetDescription_ChangedSource_ExistingTarget() throws Exception {
		String sourceDescription = "changed source description";
		String targetDescription = "{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nsource description\n{panel}\ntarget description";
		String description = descriptionMapper.mapTargetDescription(sourceDescription, targetDescription);
		assertThat(description, is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nchanged source description\n{panel}\n\ntarget description"));
	}

	@Test
	public void testMapTargetDescription_ExistingSource_ExistingTarget() throws Exception {
		String sourceDescription = "source description $_@-äüö";
		String targetDescription = descriptionMapper.mapSourceDescription(sourceDescription).trim();
		String description = descriptionMapper.mapTargetDescription(sourceDescription, targetDescription);
		assertThat(description, is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nsource description $_@-äüö\n{panel}"));
	}

	@Test
	public void testMapTargetDescription_ChangedSource_ExistingTarget_AlsoTextAboveOriginalDescription() throws Exception {
		String sourceDescription = "changed source description";
		String targetDescription = "Some text above\n{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nsource description\n{panel}\ntarget description";
		String description = descriptionMapper.mapTargetDescription(sourceDescription, targetDescription);
		assertThat(description, is("Some text above\n{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nchanged source description\n{panel}\n\ntarget description"));
	}

	@Test
	public void testMapTargetDescription_ChangedSource_ExistingTarget_BrokenWhiteSpace() throws Exception {
		String sourceDescription = "changed source description";
		String targetDescription = "{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nsource description\n{panel}     target description";
		String description = descriptionMapper.mapTargetDescription(sourceDescription, targetDescription);
		assertThat(description, is("{panel:title=Original description|titleBGColor=#DDD|bgColor=#EEE}\nchanged source description\n{panel}\n\ntarget description"));
	}

}