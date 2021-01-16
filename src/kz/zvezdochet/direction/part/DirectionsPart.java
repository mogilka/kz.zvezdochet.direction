package kz.zvezdochet.direction.part;

import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.ui.view.ListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.CalcUtil;

/**
 * Сводная таблица дирекций планет по домам
 * @author Natalie Didenko
 */
public class DirectionsPart extends ListView {
	/**
	 * Событие
	 */
	private Event event;

	@Inject
	public DirectionsPart() {}

	@PostConstruct @Override
	public View create(Composite parent) {
		return super.create(parent);
	}

	/**
	 * Инициализация конфигурации события
	 * @param event конфигурация события
	 */
	public void setEvent(Event event) {
		this.event = event;
		if (0 == tableViewer.getTable().getColumnCount())
			addColumns();
	}

	/**
	 * Возвращает событие
	 * @return событин
	 */
	public Event getEvent() {
		return event;
	}

	@Override
	protected void addColumns() {
		if (event != null) {
			Table table = tableViewer.getTable();
			TableColumn tableColumn = new TableColumn(table, SWT.NONE);
			tableColumn.setText("Астрологический дом");
			Collection<Planet> planets = event.getPlanets().values();		
			for (Planet planet : planets) {
				tableColumn = new TableColumn(table, SWT.NONE);
				tableColumn.setText(CalcUtil.roundTo(planet.getLongitude(), 1) + "");
				tableColumn.setImage(planet.getImage());
				tableColumn.setToolTipText(planet.getName());
			}
		}
	}

	@Override
	protected String[] initTableColumns() {
		return null;
	}
}
