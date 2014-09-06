package kz.zvezdochet.direction.part;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.provider.DictionaryLabelProvider;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelLabelProvider;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.service.HouseService;
import kz.zvezdochet.service.PlanetService;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

/**
 * Поиск событий на указанный возраст
 * @author Nataly Didenko
 */
public class AgePart extends ModelListView {
	private Spinner spFrom;
	private Spinner spTo;
	private ComboViewer cvPlanet;
	private Combo cmbPlanet;
	private ComboViewer cvHouse;
	private Combo cmbHouse;
	
	@Inject
	public AgePart() {
		
	}

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
		lb.setText("Возраст");
		spFrom = new Spinner(grFilter, SWT.BORDER);
		spFrom.setMinimum(0);
		spFrom.setMaximum(150);
		spFrom.setSelection(20);

		spTo = new Spinner(grFilter, SWT.BORDER);
		spTo.setMinimum(0);
		spTo.setMaximum(150);
		spTo.setSelection(20);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Сфера жизни");
		cvPlanet = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);
		cmbPlanet = cvPlanet.getCombo();

		cvHouse = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);
		cmbHouse = cvHouse.getCombo();

		GridLayoutFactory.swtDefaults().numColumns(6).applyTo(grFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grFilter);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(spFrom);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(spTo);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cmbPlanet);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cmbHouse);
	}

	@Override
	protected String[] initTableColumns() {
		String[] columns = {
			"Возраст",
			"",
			"",
			"",
			"" };
		return columns;
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ModelLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				SkyPointAspect aspect = (SkyPointAspect)element;
				switch (columnIndex) {
					case 0: return String.valueOf(aspect.getScore());
					case 1: return aspect.getSkyPoint1().getName();
					case 2: return aspect.getAspect().getName();
					case 3: return aspect.getSkyPoint2().getName();
					case 4: return aspect.isRetro() ? "R" : "";
				}
				return null;
			}
			@Override
			public Image getColumnImage(Object element, int columnIndex) {
				SkyPointAspect aspect = (SkyPointAspect)element;
				switch (columnIndex) {
					case 1: return aspect.getSkyPoint1() instanceof Planet
							? ((Planet)aspect.getSkyPoint1()).getImage() : null;
					case 3: return aspect.getSkyPoint2() instanceof Planet
							? ((Planet)aspect.getSkyPoint2()).getImage() : null;
				}
				return null;
			}
			@Override
			public Color getForeground(Object element, int columnIndex) {
				if (2 == columnIndex) {
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
		try {
			cvPlanet.setContentProvider(new ArrayContentProvider());
			cvPlanet.setLabelProvider(new DictionaryLabelProvider());
			List<Model> list = new PlanetService().getList();
			Planet planet = new Planet();
			planet.setId(0L);
			list.add(planet);
			cvPlanet.setInput(list);

			cvHouse.setContentProvider(new ArrayContentProvider());
			cvHouse.setLabelProvider(new DictionaryLabelProvider());
			list = new HouseService().getList();
			House house = new House();
			house.setId(0L);
			list.add(house);
			cvHouse.setInput(list);
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean check(int mode) {
		if (spFrom.getSelection() > spTo.getSelection()) {
			DialogUtil.alertError("Укажите правильный период жизни");
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
	
	public int getInitialAge() {
		return spFrom.getSelection();
	}

	public int getFinalAge() {
		return spTo.getSelection();
	}

	public Planet getPlanet() {
		IStructuredSelection selection = (IStructuredSelection)cvPlanet.getSelection();
		if (selection.getFirstElement() != null) {
			Planet planet = (Planet)selection.getFirstElement();
			if (planet.getId() > 0)
				return planet;
		}
		return null;
	}

	public House getHouse() {
		IStructuredSelection selection = (IStructuredSelection)cvHouse.getSelection();
		if (selection.getFirstElement() != null) {
			House house = (House)selection.getFirstElement();
			if (house.getId() > 0)
				return house;
		}
		return null;
	}
}
