package com.rinke.solutions.pinball;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import com.rinke.solutions.pinball.ColumnLabelProviderAdapter.LabelSupplier;

@RunWith(MockitoJUnitRunner.class)
public class ColumnLabelProviderAdapterTest {
	@Mock
	private LabelSupplier labelSupplier;
	@InjectMocks
	private ColumnLabelProviderAdapter columnLabelProviderAdapter;

	@Test
	public void testGetText() throws Exception {
		when(labelSupplier.getLabel(eq("Foo"))).thenReturn("bar");
		String text = columnLabelProviderAdapter.getText("Foo");
		assertThat(text, equalTo("bar"));
	}

}
