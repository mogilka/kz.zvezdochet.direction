package kz.zvezdochet.direction.part;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.provider.DictionaryLabelProvider;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.direction.provider.TransitLabelProvider;
import kz.zvezdochet.service.AspectTypeService;
import kz.zvezdochet.service.HouseService;
import kz.zvezdochet.service.PlanetService;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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
	private ComboViewer cvHouse;
	private ComboViewer cvAspect;
	private Button btRetro;

	@Inject
	public AgePart() {}

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

		spTo = new Spinner(grFilter, SWT.BORDER);
		spTo.setMinimum(0);
		spTo.setMaximum(150);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Сфера жизни");
		cvPlanet = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);
		cvHouse = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Аспекты");
		cvAspect = new ComboViewer(grFilter, SWT.READ_ONLY | SWT.BORDER);

		btRetro = new Button(grFilter, SWT.BORDER | SWT.CHECK);
		lb = new Label(grFilter, SWT.NONE);
		lb.setText("R");
		lb.setToolTipText("Включая ретроградные аспекты");

		GridLayoutFactory.swtDefaults().numColumns(10).applyTo(grFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grFilter);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(spFrom);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(spTo);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvPlanet.getCombo());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvHouse.getCombo());
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(cvAspect.getCombo());
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).
			grab(false, false).applyTo(btRetro);
	}

	@Override
	protected String[] initTableColumns() {
		return TransitPart.getTableColumns();
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new TransitLabelProvider();
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
		spFrom.setSelection(0);
		spTo.setSelection(event.getAge());
	}

	@Override
	protected void initControls() {
		try {
			cvPlanet.setContentProvider(new ArrayContentProvider());
			cvPlanet.setLabelProvider(new DictionaryLabelProvider());
			List<Model> list = new PlanetService().getList();
			Planet planet = new Planet();
			planet.setId(0L);
			list.add(0, planet);
			cvPlanet.setInput(list);

			cvHouse.setContentProvider(new ArrayContentProvider());
			cvHouse.setLabelProvider(new DictionaryLabelProvider());
			list = new HouseService().getList();
			House house = new House();
			house.setId(0L);
			list.add(0, house);
			cvHouse.setInput(list);

			cvAspect.setContentProvider(new ArrayContentProvider());
			cvAspect.setLabelProvider(new DictionaryLabelProvider());
			list = new AspectTypeService().getList();
			AspectType aspect = new AspectType();
			aspect.setId(0L);
			list.add(0, aspect);
			cvAspect.setInput(list);
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

	/**
	 * Возвращает выбранный начальный возраст
	 * @return начальный возраст
	 */
	public int getInitialAge() {
		return spFrom.getSelection();
	}

	/**
	 * Возвращает выбранный конечный возраст
	 * @return конечный возраст
	 */
	public int getFinalAge() {
		return spTo.getSelection();
	}

	/**
	 * Возвращает выбранную планету как сферу жизни
	 * @return планета
	 */
	public Planet getPlanet() {
		IStructuredSelection selection = (IStructuredSelection)cvPlanet.getSelection();
		if (selection.getFirstElement() != null) {
			Planet planet = (Planet)selection.getFirstElement();
			if (planet.getId() > 0)
				return planet;
		}
		return null;
	}

	/**
	 * Возвращает выбранный дом как сферу жизни
	 * @return астрологический дом
	 */
	public House getHouse() {
		IStructuredSelection selection = (IStructuredSelection)cvHouse.getSelection();
		if (selection.getFirstElement() != null) {
			House house = (House)selection.getFirstElement();
			if (house.getId() > 0)
				return house;
		}
		return null;
	}

	/**
	 * Возвращает выбранный тип аспекта
	 * @return тип аспекта
	 */
	public AspectType getAspect() {
		IStructuredSelection selection = (IStructuredSelection)cvAspect.getSelection();
		if (selection.getFirstElement() != null) {
			AspectType type = (AspectType)selection.getFirstElement();
			if (type.getId() > 0)
				return type;
		}
		return null;
	}

	/**
	 * Возвращает выбранное направление аспектов
	 * @return true|false только директные|включая попятные
	 */
	public boolean getRetro() {
		return btRetro.getSelection();
	}

	@Override
	public Model createModel() {
		return null;
	}
}
