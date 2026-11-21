import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';

const STORAGE_KEY = 'novacart.cart';
const CartContext = createContext(null);

function readCart() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function CartProvider({ children }) {
  const [items, setItems] = useState(readCart);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  }, [items]);

  const addItem = useCallback((product, quantity = 1) => {
    setItems((current) => {
      const existing = current.find((item) => item.productId === product.id);
      const available = product.quantity ?? 0;

      if (existing) {
        const nextQuantity = Math.min(existing.quantity + quantity, available || existing.quantity);
        return current.map((item) =>
          item.productId === product.id ? { ...item, quantity: nextQuantity } : item
        );
      }

      return [
        ...current,
        {
          productId: product.id,
          name: product.name,
          price: Number(product.price),
          imageUrl: product.imageUrl,
          quantity: Math.min(quantity, available || quantity),
          available
        }
      ];
    });
  }, []);

  const setQuantity = useCallback((productId, quantity) => {
    setItems((current) =>
      current
        .map((item) => {
          if (item.productId !== productId) {
            return item;
          }

          const cap = item.available || quantity;
          return { ...item, quantity: Math.max(0, Math.min(quantity, cap)) };
        })
        .filter((item) => item.quantity > 0)
    );
  }, []);

  const removeItem = useCallback((productId) => {
    setItems((current) => current.filter((item) => item.productId !== productId));
  }, []);

  const removeItems = useCallback((productIds) => {
    const ids = new Set(Array.isArray(productIds) ? productIds : []);
    setItems((current) => current.filter((item) => !ids.has(item.productId)));
  }, []);

  const clearCart = useCallback(() => setItems([]), []);

  const totalItems = useMemo(
    () => items.reduce((total, item) => total + item.quantity, 0),
    [items]
  );

  const subtotal = useMemo(
    () => items.reduce((total, item) => total + item.price * item.quantity, 0),
    [items]
  );

  const value = useMemo(
    () => ({
      items,
      totalItems,
      subtotal,
      addItem,
      setQuantity,
      removeItem,
      removeItems,
      clearCart
    }),
    [items, totalItems, subtotal, addItem, setQuantity, removeItem, removeItems, clearCart]
  );

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart() {
  const context = useContext(CartContext);

  if (!context) {
    throw new Error('useCart must be used inside CartProvider');
  }

  return context;
}
