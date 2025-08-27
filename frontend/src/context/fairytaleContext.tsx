"use client";

export interface Fairytale {
  id: number;
  userId: number;
  title: string;
  content: string;
  imageUrl?: string;
  isPublic: boolean;
  createdAt: string;

  childName: string;
  childRole: string;
  characters: string;
  place: string;
  lesson: string;
  mood: string;
}

export interface FairytaleWithBookmark extends Fairytale {
  isBookmarked: boolean;
}