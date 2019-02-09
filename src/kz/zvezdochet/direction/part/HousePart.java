package kz.zvezdochet.direction.part;

import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.ui.view.ListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.util.Configuration;

/**
 * Дирекции планет по домам
 * @author Nataly Didenko
 */
public class HousePart extends ListView {
	/**
	 * Конфигурация события
	 */
	private Configuration conf;

	@Inject
	public HousePart() {
		
	}

	@PostConstruct @Override
	public View create(Composite parent) {
		return super.create(parent);
	}

	/**
	 * Инициализация конфигурации события
	 * @param configuration конфигурация события
	 */
	public void setConfiguration(Configuration configuration) {
		conf = configuration;
		if (0 == table.getColumnCount())
			addColumns();
	}

	/**
	 * Возвращает конфигурацию события
	 * @return конфигурация
	 */
	public Configuration getConfiguration() {
		return conf;
	}

	@Override
	protected void addColumns() {
		if (conf != null) {
			TableColumn tableColumn = new TableColumn(table, SWT.NONE);
			tableColumn.setText("Астрологический дом");
			Collection<Planet> planets = conf.getPlanets().values();		
			for (Planet planet : planets) {
				tableColumn = new TableColumn(table, SWT.NONE);
				tableColumn.setText(CalcUtil.roundTo(planet.getCoord(), 1) + "");
				tableColumn.setImage(planet.getImage());
				tableColumn.setToolTipText(planet.getName());
			}
		}
	}

	@Override
	protected String[] initTableColumns() {
		return null;
	}

	@Override
	public boolean check(int mode) throws Exception {
		return false;
	}
}
