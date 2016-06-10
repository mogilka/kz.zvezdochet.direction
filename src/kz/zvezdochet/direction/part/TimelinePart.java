package kz.zvezdochet.direction.part;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.direction.provider.TransitLabelProvider;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.widgets.Composite;

/**
 * Таймлайн события
 * @author Nataly Didenko
 */
public class TimelinePart extends ModelListView {

	@Inject
	public TimelinePart() {}

	@PostConstruct @Override
	public View create(Composite parent) {
		super.create(parent);
		return null;
	}

	@Override
	protected String[] initTableColumns() {
		return TransitPart.getTableColumns();
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new TransitLabelProvider();
	}

	@Override
	public boolean check(int mode) throws Exception {
		return false;
	}

}