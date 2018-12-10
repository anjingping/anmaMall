package fun.anma.cart.service;

import java.util.List;

import fun.anma.common.pojo.AnmaResult;
import fun.anma.common.pojo.CartItem;

public interface CartService {
	//添加商品到购物车，将信息存到redis
	public AnmaResult addItemCart(String token,Long itemId,Integer num);
	//更具+-改变购物车数量
	public AnmaResult updateItemNum(String token,Long itemId,Integer num);
	//删除购物车商品信息
	public AnmaResult deleteItem(String token,Long itemId);
	//获取购物车的所有商品信息
	public List<CartItem> showCartList(String token);
}
