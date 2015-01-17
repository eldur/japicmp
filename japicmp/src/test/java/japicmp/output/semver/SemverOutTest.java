package japicmp.output.semver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import japicmp.model.AccessModifier;
import japicmp.model.JApiBinaryCompatibility;
import japicmp.model.JApiChangeStatus;
import japicmp.model.JApiHasAccessModifier;
import japicmp.model.JApiHasChangeStatus;
import japicmp.model.JApiModifier;
import org.junit.Test;

public class SemverOutTest {

	@Test
	public void testUnchanged() {
		// given
		JApiChangeStatus changeStatus = JApiChangeStatus.UNCHANGED;
		AccessModifier oldAccess = null;
		AccessModifier newAccess = null;
		Boolean isBinaryCompatible = Boolean.FALSE;

		TestData testData = newTestData(changeStatus, oldAccess, newAccess, isBinaryCompatible);

		// when
		SemverOut.SemverStatus status =
				SemverOut.detectChangeStatus(testData.hasChangeStatus, testData.modifier);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testNewPrivate() {
		// given
		JApiChangeStatus changeStatus = JApiChangeStatus.NEW;
		AccessModifier oldAccess = null;
		AccessModifier newAccess = AccessModifier.PRIVATE;
		Boolean isBinaryCompatible = Boolean.TRUE;

		TestData testData = newTestData(changeStatus, oldAccess, newAccess, isBinaryCompatible);

		// when
		SemverOut.SemverStatus status =
				SemverOut.detectChangeStatus(testData.hasChangeStatus, testData.modifier);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testNewProtectedIncompatible() {
		// given
		JApiChangeStatus changeStatus = JApiChangeStatus.NEW;
		AccessModifier oldAccess = null;
		AccessModifier newAccess = AccessModifier.PROTECTED;
		Boolean isBinaryCompatible = Boolean.FALSE;

		TestData testData = newTestData(changeStatus, oldAccess, newAccess, isBinaryCompatible);

		// when
		SemverOut.SemverStatus status =
				SemverOut.detectChangeStatus(testData.hasChangeStatus, testData.modifier);

		// then
		assertEquals(SemverOut.SemverStatus.MAJOR, status);
	}

	@Test
	public void testFailMissingInterface() {
		JApiHasChangeStatus hasChangeStatus = mock(JApiHasChangeStatus.class);
		when(hasChangeStatus.getChangeStatus()).thenReturn(JApiChangeStatus.MODIFIED);
		try {
			SemverOut.detectChangeStatus(hasChangeStatus, null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("change status must implement " + //
							JApiBinaryCompatibility.class.getCanonicalName(), e.getMessage());
		}
	}

	@Test
	public void testFailMissingModifier() {
		ChangeWithBinaryCompatibility hasChangeStatus = mock(ChangeWithBinaryCompatibility.class);
		when(hasChangeStatus.getChangeStatus()).thenReturn(JApiChangeStatus.MODIFIED);
		when(hasChangeStatus.isBinaryCompatible()).thenReturn(true);
		try {
			SemverOut.detectChangeStatus(hasChangeStatus, null);
			fail();
		} catch (IllegalStateException e) {
			assertEquals("access modifier must not be null", e.getMessage());
		}
	}

	@Test
	public void testChangeStatusFromAccessModifier_new_private() {
		// given
		AccessModifier oldAccess = null;
		AccessModifier newAccess = AccessModifier.PRIVATE;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_new_package_protected() {
		// given
		AccessModifier oldAccess = null;
		AccessModifier newAccess = AccessModifier.PACKAGE_PROTECTED;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_new_protected() {
		// given
		AccessModifier oldAccess = null;
		AccessModifier newAccess = AccessModifier.PROTECTED;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status); // XXX check
	}

	@Test
	public void testChangeStatusFromAccessModifier_new_public() {
		// given
		AccessModifier oldAccess = null;
		AccessModifier newAccess = AccessModifier.PUBLIC;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.MINOR, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_removed_public() {
		// given
		AccessModifier oldAccess = AccessModifier.PUBLIC;
		AccessModifier newAccess = null;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.MAJOR, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_removed_protected() {
		// given
		AccessModifier oldAccess = AccessModifier.PROTECTED;
		AccessModifier newAccess = null;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status); // XXX check
	}

	@Test
	public void testChangeStatusFromAccessModifier_removed_package_protected() {
		// given
		AccessModifier oldAccess = AccessModifier.PACKAGE_PROTECTED;
		AccessModifier newAccess = null;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_removed_private() {
		// given
		AccessModifier oldAccess = AccessModifier.PRIVATE;
		AccessModifier newAccess = null;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_private_to_private() {
		// given
		AccessModifier oldAccess = AccessModifier.PRIVATE;
		AccessModifier newAccess = AccessModifier.PRIVATE;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_private_to_package_protected() {
		// given
		AccessModifier oldAccess = AccessModifier.PRIVATE;
		AccessModifier newAccess = AccessModifier.PACKAGE_PROTECTED;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_private_to_protected() {
		// given
		AccessModifier oldAccess = AccessModifier.PRIVATE;
		AccessModifier newAccess = AccessModifier.PROTECTED;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status); // XXX MINOR?
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_private_to_public() {
		// given
		AccessModifier oldAccess = AccessModifier.PRIVATE;
		AccessModifier newAccess = AccessModifier.PUBLIC;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.MINOR, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_package_protected_to_private() {
		// given
		AccessModifier oldAccess = AccessModifier.PACKAGE_PROTECTED;
		AccessModifier newAccess = AccessModifier.PRIVATE;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_package_protected_to_package_protected() {
		// given
		AccessModifier oldAccess = AccessModifier.PACKAGE_PROTECTED;
		AccessModifier newAccess = AccessModifier.PACKAGE_PROTECTED;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_package_protected_to_protected() {
		// given
		AccessModifier oldAccess = AccessModifier.PACKAGE_PROTECTED;
		AccessModifier newAccess = AccessModifier.PROTECTED;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status); // XXX MINOR?
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_package_protected_to_public() {
		// given
		AccessModifier oldAccess = AccessModifier.PACKAGE_PROTECTED;
		AccessModifier newAccess = AccessModifier.PUBLIC;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.MINOR, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_protected_to_private() {
		// given
		AccessModifier oldAccess = AccessModifier.PROTECTED;
		AccessModifier newAccess = AccessModifier.PRIVATE;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status); // XXX MAJOR?
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_protected_to_package_protected() {
		// given
		AccessModifier oldAccess = AccessModifier.PROTECTED;
		AccessModifier newAccess = AccessModifier.PACKAGE_PROTECTED;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status); // XXX MAJOR?
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_protected_to_protected() {
		// given
		AccessModifier oldAccess = AccessModifier.PROTECTED;
		AccessModifier newAccess = AccessModifier.PROTECTED;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_protected_to_public() {
		// given
		AccessModifier oldAccess = AccessModifier.PROTECTED;
		AccessModifier newAccess = AccessModifier.PUBLIC;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.MINOR, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_public_to_private() {
		// given
		AccessModifier oldAccess = AccessModifier.PUBLIC;
		AccessModifier newAccess = AccessModifier.PRIVATE;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.MAJOR, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_public_to_package_protected() {
		// given
		AccessModifier oldAccess = AccessModifier.PUBLIC;
		AccessModifier newAccess = AccessModifier.PACKAGE_PROTECTED;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.MAJOR, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_public_to_protected() {
		// given
		AccessModifier oldAccess = AccessModifier.PUBLIC;
		AccessModifier newAccess = AccessModifier.PROTECTED;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.MAJOR, status);
	}

	@Test
	public void testChangeStatusFromAccessModifier_modified_public_to_public() {
		// given
		AccessModifier oldAccess = AccessModifier.PUBLIC;
		AccessModifier newAccess = AccessModifier.PUBLIC;

		JApiHasAccessModifier testData = newModifier(oldAccess, newAccess);

		// when
		SemverOut.SemverStatus status = SemverOut.changeStatusFromAccessModifier(testData);

		// then
		assertEquals(SemverOut.SemverStatus.PATCH, status);
	}

	private TestData newTestData(JApiChangeStatus changeStatus, AccessModifier oldAccess,
			AccessModifier newAccess, Boolean isBinaryCompatible) {

		final JApiHasChangeStatus hasChangeStatus;
		hasChangeStatus = mock(ChangeWithBinaryCompatibility.class);
		if (isBinaryCompatible) {
			when(((ChangeWithBinaryCompatibility) hasChangeStatus).isBinaryCompatible())
					.thenReturn(isBinaryCompatible);
		}

		when(hasChangeStatus.getChangeStatus()).thenReturn(changeStatus);

		JApiHasAccessModifier modifier = newModifier(oldAccess, newAccess);

		return new TestData(hasChangeStatus, modifier);
	}

	private JApiHasAccessModifier newModifier(AccessModifier oldAccess, AccessModifier newAccess) {
		Optional<AccessModifier> oldMod = Optional.fromNullable(oldAccess);
		Optional<AccessModifier> newMod = Optional.fromNullable(newAccess);

		JApiModifier<AccessModifier> accessModifier = mock(JApiModifier.class);
		when(accessModifier.getOldModifier()).thenReturn(oldMod);
		when(accessModifier.getNewModifier()).thenReturn(newMod);

		JApiHasAccessModifier modifier = mock(JApiHasAccessModifier.class);
		when(accessModifier.getChangeStatus()).thenReturn(JApiChangeStatus.MODIFIED); // XXX check
		when(modifier.getAccessModifier()).thenReturn(accessModifier);
		return modifier;
	}

	private static class TestData {

		private final JApiHasChangeStatus hasChangeStatus;
		private final JApiHasAccessModifier modifier;

		public TestData(JApiHasChangeStatus hasChangeStatus, JApiHasAccessModifier modifier) {
			this.hasChangeStatus = hasChangeStatus;
			this.modifier = modifier;
		}
	}

	private static interface ChangeWithBinaryCompatibility
			extends JApiBinaryCompatibility, JApiHasChangeStatus {

	}
}
