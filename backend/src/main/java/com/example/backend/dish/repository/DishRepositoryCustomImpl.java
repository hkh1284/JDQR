package com.example.backend.dish.repository;

import static com.example.backend.dish.entity.QDish.*;
import static com.example.backend.dish.entity.QDishCategory.*;

import java.util.List;

import com.example.backend.common.repository.Querydsl4RepositorySupport;
import com.example.backend.dish.entity.Dish;
import com.example.backend.dish.entity.QDish;
import com.example.backend.dish.entity.QDishCategory;
import com.example.backend.etc.entity.Restaurant;

public class DishRepositoryCustomImpl extends Querydsl4RepositorySupport implements DishRepositoryCustom {
	@Override
	public List<Dish> findDishesByRestaurant(Restaurant restaurant) {
		return selectFrom(dish)
			.join(dish.dishCategory, dishCategory).fetchJoin()
			.where(dishCategory.restaurant.eq(restaurant))
			.fetch();
	}
}
