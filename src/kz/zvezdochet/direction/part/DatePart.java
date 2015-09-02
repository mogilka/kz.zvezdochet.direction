package kz.zvezdochet.direction.part;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.ui.decoration.RequiredDecoration;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelLabelProvider;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

/**
 * Таблица транзитов на указанную дату
 * @author Nataly Didenko
 */
public class DatePart extends ModelListView {
	private CDateTime dtBirth;

	@Inject
	public DatePart() {}

	@PostConstruct @Override
	public View create(Composite parent) {
		super.create(parent);
		return null;
	}
	
	@Override
	public void initFilter() {
		grFilter = new Group(container, SWT.NONE);
		grFilter.setText("Поиск");

		Label lb = new Label(grFilter, SWT.NONE);
		lb.setText("Дата");
		dtBirth = new CDateTime(grFilter, CDT.BORDER | CDT.COMPACT | CDT.DROP_DOWN | CDT.DATE_LONG | CDT.TIME_MEDIUM);
		dtBirth.setNullText(""); //$NON-NLS-1$
		new RequiredDecoration(lb, SWT.TOP | SWT.RIGHT);

		GridLayoutFactory.swtDefaults().numColumns(10).applyTo(grFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grFilter);
		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.CENTER).
			grab(false, false).applyTo(dtBirth);
	}

	@Override
	protected String[] initTableColumns() {
		String[] columns = {
			"Точка 1",
			"Аспект",
			"Точка 2",
			"Величина аспекта" };
		return columns;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ModelLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				SkyPointAspect aspect = (SkyPointAspect)element;
				switch (columnIndex) {
					case 0: return aspect.getSkyPoint1().getName();
					case 1: return aspect.getAspect().getName();
					case 2: return aspect.getSkyPoint2().getName();
					case 3: return String.valueOf(aspect.getScore());
				}
				return null;
			}
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				SkyPointAspect aspect = (SkyPointAspect)element;
				switch (columnIndex) {
					case 0: return aspect.getSkyPoint1() instanceof Planet
							? ((Planet)aspect.getSkyPoint1()).getImage() : null;
					case 2: return aspect.getSkyPoint2() instanceof Planet
							? ((Planet)aspect.getSkyPoint2()).getImage() : null;
				}
				return null;
			}
			@Override
			public Color getForeground(Object element, int columnIndex) {
				if (1 == columnIndex) {
					SkyPointAspect aspect = (SkyPointAspect)element;
					if (aspect.getAspect() != null)
						return aspect.getAspect().getType().getColor();
				}
				return  Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
			}
		};
	}

	private Event event;

	/**
	 * Поиск персоны, для которой делается прогноз
	 * @return персона
	 */
	public Event getEvent() {
		return event;
	}

	/**
	 * Инициализация персоны, для которой делается прогноз
	 * @param event персона
	 */
	public void setEvent(Event event) {
		this.event = event;
	}

	@Override
	protected void initControls() {
		dtBirth.setSelection(new Date());
	}

	@Override
	public boolean check(int mode) {
		if (null == dtBirth.getSelection()) {
			DialogUtil.alertError("Укажите дату");
			return false;
		} else if (null == event) {
			DialogUtil.alertError("Событие не задано");
			return false;
		} else if (null == event.getConfiguration()) {
			DialogUtil.alertError("Конфигурация события не задана");
			return false;
		}
		return true;
	}

	/**
	 * Возвращает выбранную дату
	 * @return дата транзита
	 */
	public Date getDate() {
		return dtBirth.getSelection();
	}
}
