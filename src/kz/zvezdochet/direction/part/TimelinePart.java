package kz.zvezdochet.direction.part;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.swt.widgets.Composite;

import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.direction.provider.TransitLabelProvider;

/**
 * Таймлайн события
 * @author Natalie Didenko
 */
public class TimelinePart extends ModelListView {

	@Inject
	public TimelinePart() {}

	@PostConstruct @Override
	public View create(Composite parent) {
		return super.create(parent);
	}

	@Override
	protected String[] initTableColumns() {
		return EventPart.getTableColumns();
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new TransitLabelProvider();
	}

	@Override
	public boolean check(int mode) throws Exception {
		return false;
	}

	@Override
	public Model createModel() {
		return null;
	}

}
