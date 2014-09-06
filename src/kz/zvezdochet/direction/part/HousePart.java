package kz.zvezdochet.direction.part;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.ui.view.ListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.util.Configuration;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

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
		addColumns();
	}

	@Override
	protected void addColumns() {
		if (conf != null) {
			TableColumn tableColumn = new TableColumn(table, SWT.NONE);
			tableColumn.setText("Астрологический дом");		
			for (int i = 0; i < conf.getPlanets().size(); i++) {
				Planet planet = (Planet)conf.getPlanets().get(i);
				tableColumn = new TableColumn(table, SWT.NONE);
				tableColumn.setText(planet.getName() + " (" + CalcUtil.roundTo(planet.getCoord(), 1) + ")");		
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
